package org.batfish.question.testfilters;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
import org.batfish.datamodel.FilterResult;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.Flow.Builder;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.PacketHeaderConstraints;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.UniverseIpSpace;
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
import org.batfish.specifier.FlexibleInferFromLocationIpSpaceSpecifierFactory;
import org.batfish.specifier.InterfaceLinkLocation;
import org.batfish.specifier.InterfaceLocation;
import org.batfish.specifier.IpSpaceAssignment;
import org.batfish.specifier.IpSpaceAssignment.Entry;
import org.batfish.specifier.IpSpaceSpecifier;
import org.batfish.specifier.IpSpaceSpecifierFactory;
import org.batfish.specifier.Location;
import org.batfish.specifier.LocationSpecifier;
import org.batfish.specifier.LocationVisitor;
import org.batfish.specifier.SpecifierContext;

public class TestFiltersAnswerer extends Answerer {

  private static final String IP_SPECIFIER_FACTORY =
      FlexibleInferFromLocationIpSpaceSpecifierFactory.NAME;

  public static final String COL_NODE = "Node";
  public static final String COL_FILTER_NAME = "Filter_Name";
  public static final String COL_FLOW = "Flow";
  public static final String COL_ACTION = "Action";
  public static final String COL_LINE_NUMBER = "Line_Number";
  public static final String COL_LINE_CONTENT = "Line_Content";
  public static final String COL_TRACE = "Trace";

  private final IpSpaceRepresentative _ipSpaceRepresentative;
  private final IpSpaceAssignment _sourceIpAssignment;

  public TestFiltersAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
    _ipSpaceRepresentative = new IpSpaceRepresentative();
    _sourceIpAssignment =
        initSourceIpAssignment((TestFiltersQuestion) question, batfish.specifierContext());
  }

  @VisibleForTesting
  static IpSpaceAssignment initSourceIpAssignment(
      TestFiltersQuestion question, SpecifierContext ctxt) {
    /* construct specifiers */
    LocationSpecifier sourceLocationSpecifier = question.getStartLocationSpecifier();

    IpSpaceSpecifier sourceIpSpaceSpecifier =
        IpSpaceSpecifierFactory.load(IP_SPECIFIER_FACTORY)
            .buildIpSpaceSpecifier(question.getHeaders().getSrcIps());

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
            new ColumnMetadata(COL_LINE_NUMBER, Schema.INTEGER, "Line number", false, true),
            new ColumnMetadata(COL_LINE_CONTENT, Schema.STRING, "Line content", false, true),
            new ColumnMetadata(COL_TRACE, Schema.ACL_TRACE, "ACL trace", false, true));
    String textDesc =
        String.format(
            "Filter ${%s} on node ${%s} will ${%s} flow ${%s} at line ${%s} ${%s}",
            COL_FILTER_NAME, COL_NODE, COL_ACTION, COL_FLOW, COL_LINE_NUMBER, COL_LINE_CONTENT);
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

  private Set<Flow> getFlows(Configuration c, ImmutableSet.Builder<String> allProblems) {
    TestFiltersQuestion question = (TestFiltersQuestion) _question;
    String node = c.getHostname();
    Set<Location> srcLocations =
        question
            .getStartLocationSpecifier()
            .resolve(_batfish.specifierContext())
            .stream()
            .filter(LocationVisitor.onNode(node)::visit)
            .collect(Collectors.toSet());

    ImmutableSet.Builder<Flow> setBuilder = ImmutableSet.builder();

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
        setSourceLocation(flowBuilder, srcLocation, c);
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
        .put(COL_LINE_NUMBER, matchLine)
        .put(COL_LINE_CONTENT, lineDesc)
        .put(COL_TRACE, trace)
        .build();
  }

  @VisibleForTesting
  Multiset<Row> getRows() {
    TestFiltersQuestion question = (TestFiltersQuestion) _question;
    Map<String, Configuration> configurations = _batfish.loadConfigurations();
    Set<String> includeNodes = question.getNodes().getMatchingNodes(_batfish);
    FilterSpecifier filterSpecifier = question.getFilterSpecifier();

    Multiset<Row> rows = HashMultiset.create();

    // collect all errors while building flows; return this set when no valid flow is found
    ImmutableSet.Builder<String> allProblems = ImmutableSet.builder();

    for (String node : includeNodes) {
      Configuration c = configurations.get(node);
      Set<Flow> flows = getFlows(c, allProblems);

      // there should be another for loop for v6 filters when we add v6 support
      for (IpAccessList filter : filterSpecifier.resolve(node, _batfish.specifierContext())) {
        for (Flow flow : flows) {
          rows.add(getRow(filter, flow, c));
        }
      }
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
  @VisibleForTesting
  Flow.Builder headerConstraintsToFlow(PacketHeaderConstraints constraints, Location srcLocation)
      throws IllegalArgumentException {
    Flow.Builder builder = Flow.builder();

    // Extract source IP from header constraints,
    String headerSrcIp = constraints.getSrcIps();
    if (headerSrcIp != null) {
      // interpret given Src IP using sane mode
      IpSpaceSpecifier srcIpSpecifier =
          IpSpaceSpecifierFactory.load(IP_SPECIFIER_FACTORY).buildIpSpaceSpecifier(headerSrcIp);
      IpSpaceAssignment srcIps =
          srcIpSpecifier.resolve(ImmutableSet.of(), _batfish.specifierContext());
      checkArgument(
          srcIps.getEntries().size() == 1,
          "Specified source: %s, resolves to more than one IP",
          headerSrcIp);
      IpSpace space = srcIps.getEntries().iterator().next().getIpSpace();
      Optional<Ip> srcIp = _ipSpaceRepresentative.getRepresentative(space);
      checkArgument(srcIp.isPresent(), "At least one source IP is required");
      builder.setSrcIp(srcIp.get());
    } else if (srcLocation == null) {
      Optional<Ip> srcIp = _ipSpaceRepresentative.getRepresentative(UniverseIpSpace.INSTANCE);
      builder.setSrcIp(
          srcIp.orElseThrow(
              () -> new IllegalArgumentException("Failed to pick an IP for unspecified location")));
    } else {
      // Use from source location to determine header Src IP
      Optional<Entry> entry =
          _sourceIpAssignment
              .getEntries()
              .stream()
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

    String headerDstIp = constraints.getDstIps();
    if (headerDstIp != null) {
      IpSpaceSpecifier dstIpSpecifier =
          IpSpaceSpecifierFactory.load(IP_SPECIFIER_FACTORY).buildIpSpaceSpecifier(headerDstIp);
      IpSpaceAssignment dstIps =
          dstIpSpecifier.resolve(ImmutableSet.of(), _batfish.specifierContext());
      checkArgument(
          dstIps.getEntries().size() == 1,
          "Specified destination: %s, resolves to more than one IP",
          headerDstIp);
      IpSpace space = dstIps.getEntries().iterator().next().getIpSpace();
      Optional<Ip> dstIp = _ipSpaceRepresentative.getRepresentative(space);
      checkArgument(dstIp.isPresent(), "At least one destination IP is required");
      builder.setDstIp(dstIp.get());
    } else {
      Optional<Ip> dstIp = _ipSpaceRepresentative.getRepresentative(UniverseIpSpace.INSTANCE);
      checkArgument(dstIp.isPresent(), "At least one destination IP is required");
      builder.setDstIp(dstIp.get());
    }

    // Deal with IP packet header values.
    // IP protocol (default to UDP)
    Set<IpProtocol> ipProtocols = constraints.resolveIpProtocols();
    checkArgument(
        ipProtocols == null || ipProtocols.size() == 1,
        "Cannot run testfilters with multiple IP protocols");
    if (ipProtocols != null) {
      builder.setIpProtocol(ipProtocols.iterator().next());
    } else {
      builder.setIpProtocol(IpProtocol.TCP);
    }

    // Src Ports
    Set<SubRange> srcPorts = constraints.getSrcPorts();
    checkArgument(
        srcPorts == null || srcPorts.size() == 1,
        "Cannot run testfilters with multiple source ports");
    if (srcPorts != null) {
      SubRange srcPort = srcPorts.iterator().next();
      builder.setSrcPort(srcPort.getStart());
    } else {
      builder.setSrcPort(NamedPort.EPHEMERAL_LOWEST.number());
    }

    // Dst Ports
    Set<SubRange> dstPorts = constraints.resolveDstPorts();
    checkArgument(
        dstPorts == null || dstPorts.size() == 1,
        "Cannot perform traceroute with multiple destination ports");
    if (dstPorts != null) {
      SubRange dstPort = dstPorts.iterator().next();
      if (dstPorts.size() > 1 || !dstPort.isSingleValue()) {
        throw new IllegalArgumentException();
      }
      builder.setDstPort(dstPort.getStart());
    } else {
      builder.setDstPort(NamedPort.HTTP.number());
    }

    // Icmp values
    Set<SubRange> icmpTypes = constraints.getIcmpTypes();
    if (icmpTypes != null) {
      SubRange icmpType = icmpTypes.iterator().next();
      if (icmpTypes.size() > 1 || !icmpType.isSingleValue()) {
        throw new IllegalArgumentException("Cannot run testfilters with multiple ICMP types");
      }
      builder.setIcmpType(icmpType.getStart());
    }
    Set<SubRange> icmpCodes = constraints.getIcmpCodes();
    if (icmpCodes != null) {
      SubRange icmpCode = icmpCodes.iterator().next();
      if (icmpCodes.size() > 1 || !icmpCode.isSingleValue()) {
        throw new IllegalArgumentException("Cannot run testfilters with multiple ICMP codes");
      }
      builder.setIcmpType(icmpCode.getStart());
    }

    // DSCP value
    Set<SubRange> dscps = constraints.getDscps();
    if (dscps != null) {
      SubRange dscp = dscps.iterator().next();
      if (dscps.size() > 1 || !dscp.isSingleValue()) {
        throw new IllegalArgumentException("Cannot run testfilters with multiple DSCP values");
      }
      builder.setDscp(dscp.getStart());
    } else {
      builder.setDscp(0);
    }

    // TODO: ECN value, fragments, etc
    return builder;
  }

  private static void setSourceLocation(Builder flowBuilder, Location loc, Configuration c) {
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
                    c.getAllInterfaces()
                        .get(interfaceLocation.getInterfaceName())
                        .getVrf()
                        .getName());
            return null;
          }
        });
  }
}

package org.batfish.question.traceroute;

import static org.batfish.datamodel.matchers.FlowMatchers.hasDstIp;
import static org.batfish.datamodel.matchers.FlowMatchers.hasIngressNode;
import static org.batfish.datamodel.matchers.FlowMatchers.hasTag;
import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
import static org.batfish.question.traceroute.TracerouteAnswerer.COL_BASE_TRACES;
import static org.batfish.question.traceroute.TracerouteAnswerer.COL_DELTA_TRACES;
import static org.batfish.question.traceroute.TracerouteAnswerer.COL_FLOW;
import static org.batfish.question.traceroute.TracerouteAnswerer.COL_TRACES;
import static org.batfish.question.traceroute.TracerouteAnswerer.COL_TRACE_COUNT;
import static org.batfish.question.traceroute.TracerouteAnswerer.diffFlowTracesToRows;
import static org.batfish.question.traceroute.TracerouteAnswerer.flowTracesToRows;
import static org.batfish.question.traceroute.TracerouteAnswerer.metadata;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multiset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.FlowHistory;
import org.batfish.datamodel.FlowHistory.FlowHistoryInfo;
import org.batfish.datamodel.FlowTrace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.flow.Hop;
import org.batfish.datamodel.flow.Trace;
import org.batfish.datamodel.matchers.TraceMatchers;
import org.batfish.datamodel.pojo.Environment;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableDiff;
import org.batfish.datamodel.table.TableMetadata;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TracerouteAnswererTest {

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testCreateMetadata() {
    TableMetadata tableMetadata = TracerouteAnswerer.createMetadata(false);
    List<String> columnNames =
        tableMetadata.getColumnMetadata().stream()
            .map(ColumnMetadata::getName)
            .collect(ImmutableList.toImmutableList());
    List<Schema> columnSchemas =
        tableMetadata.getColumnMetadata().stream()
            .map(ColumnMetadata::getSchema)
            .collect(ImmutableList.toImmutableList());

    assertThat(columnNames, equalTo(ImmutableList.of(COL_FLOW, COL_TRACES)));
    assertThat(
        columnSchemas, equalTo(ImmutableList.of(Schema.FLOW, Schema.set(Schema.FLOW_TRACE))));

    TableMetadata diffTableMetadata = TracerouteAnswerer.createMetadata(true);
    List<String> diffColumnNames =
        diffTableMetadata.getColumnMetadata().stream()
            .map(ColumnMetadata::getName)
            .collect(ImmutableList.toImmutableList());
    List<Schema> diffColumnSchemas =
        diffTableMetadata.getColumnMetadata().stream()
            .map(ColumnMetadata::getSchema)
            .collect(ImmutableList.toImmutableList());

    assertThat(
        diffColumnNames,
        equalTo(
            ImmutableList.of(
                COL_FLOW,
                TableDiff.baseColumnName(COL_TRACES),
                TableDiff.deltaColumnName(COL_TRACES))));
    assertThat(
        diffColumnSchemas,
        equalTo(
            ImmutableList.of(
                Schema.FLOW, Schema.set(Schema.FLOW_TRACE), Schema.set(Schema.FLOW_TRACE))));
  }

  @Test
  public void testDiffFlowHistoryToRows() {

    Flow flow =
        Flow.builder().setTag("tag").setIngressNode("node").setDstIp(Ip.parse("1.1.1.1")).build();
    Environment environment = new Environment(null, null, null, null, null, null, null);

    FlowHistory flowHistory = new FlowHistory();
    flowHistory.addFlowTrace(
        flow,
        Flow.BASE_FLOW_TAG,
        environment,
        new FlowTrace(FlowDisposition.ACCEPTED, ImmutableList.of(), "notes1"));
    flowHistory.addFlowTrace(
        flow,
        Flow.BASE_FLOW_TAG,
        environment,
        new FlowTrace(FlowDisposition.DENIED_OUT, ImmutableList.of(), "notes2"));
    flowHistory.addFlowTrace(
        flow,
        Flow.DELTA_FLOW_TAG,
        environment,
        new FlowTrace(FlowDisposition.ACCEPTED, ImmutableList.of(), "notes1"));
    flowHistory.addFlowTrace(
        flow,
        Flow.DELTA_FLOW_TAG,
        environment,
        new FlowTrace(FlowDisposition.ACCEPTED, ImmutableList.of(), "notes2"));

    Multiset<Row> rows = TracerouteAnswerer.flowHistoryToRows(flowHistory, true);

    assertThat(rows, hasSize(1));

    FlowHistoryInfo flowHistoryInfo = flowHistory.getTraces().get(flow.toString());

    assertThat(
        rows.iterator().next(),
        equalTo(
            Row.of(
                COL_FLOW,
                flow,
                TableDiff.baseColumnName(COL_TRACES),
                flowHistoryInfo.getPaths().get(Flow.BASE_FLOW_TAG),
                TableDiff.deltaColumnName(COL_TRACES),
                flowHistoryInfo.getPaths().get(Flow.DELTA_FLOW_TAG))));
  }

  @Test
  public void testDiffFlowTracesToRows() {
    Flow flow =
        Flow.builder().setTag("tag").setIngressNode("node").setDstIp(Ip.parse("1.1.1.1")).build();

    Map<Flow, List<Trace>> baseFlowTraces =
        ImmutableMap.of(
            flow, ImmutableList.of(new Trace(FlowDisposition.DENIED_OUT, ImmutableList.of())));
    Map<Flow, List<Trace>> deltaFlowTraces =
        ImmutableMap.of(
            flow, ImmutableList.of(new Trace(FlowDisposition.DENIED_IN, ImmutableList.of())));

    Multiset<Row> rows = diffFlowTracesToRows(baseFlowTraces, deltaFlowTraces, Integer.MAX_VALUE);
    assertThat(
        rows.iterator().next(),
        allOf(
            hasColumn(
                COL_FLOW,
                allOf(hasDstIp(Ip.parse("1.1.1.1")), hasIngressNode("node"), hasTag("tag")),
                Schema.FLOW),
            hasColumn(
                TracerouteAnswerer.COL_BASE_TRACES,
                containsInAnyOrder(
                    ImmutableList.of(TraceMatchers.hasDisposition(FlowDisposition.DENIED_OUT))),
                Schema.set(Schema.TRACE)),
            hasColumn(TracerouteAnswerer.COL_BASE_TRACE_COUNT, equalTo(1), Schema.INTEGER),
            hasColumn(
                TracerouteAnswerer.COL_DELTA_TRACES,
                containsInAnyOrder(
                    ImmutableList.of(TraceMatchers.hasDisposition(FlowDisposition.DENIED_IN))),
                Schema.set(Schema.TRACE)),
            hasColumn(TracerouteAnswerer.COL_DELTA_TRACE_COUNT, equalTo(1), Schema.INTEGER)));
  }

  @Test
  public void testFlowTracesToRowsMaxTraces() {
    Flow flow =
        Flow.builder().setTag("tag").setIngressNode("node").setDstIp(Ip.parse("1.1.1.1")).build();
    SortedMap<Flow, List<Trace>> flowTraces =
        ImmutableSortedMap.of(
            flow,
            ImmutableList.of(
                new Trace(FlowDisposition.DENIED_OUT, ImmutableList.of()),
                new Trace(FlowDisposition.DENIED_IN, ImmutableList.of())));
    Multiset<Row> rows = flowTracesToRows(flowTraces, 1);

    assertThat(
        rows.iterator().next(),
        allOf(
            hasColumn(
                COL_FLOW,
                allOf(hasDstIp(Ip.parse("1.1.1.1")), hasIngressNode("node"), hasTag("tag")),
                Schema.FLOW),
            hasColumn(COL_TRACES, hasSize(1), Schema.set(Schema.TRACE)),
            hasColumn(TracerouteAnswerer.COL_TRACE_COUNT, equalTo(2), Schema.INTEGER)));
  }

  @Test
  public void testDiffFlowTracesToRowsMaxTraces() {
    Flow flow =
        Flow.builder().setTag("tag").setIngressNode("node").setDstIp(Ip.parse("1.1.1.1")).build();

    SortedMap<Flow, List<Trace>> baseflowTraces =
        ImmutableSortedMap.of(
            flow,
            ImmutableList.of(
                new Trace(
                    FlowDisposition.DENIED_OUT,
                    ImmutableList.of(new Hop(new Node("node1"), ImmutableList.of()))),
                new Trace(
                    FlowDisposition.DENIED_IN,
                    ImmutableList.of(new Hop(new Node("node2"), ImmutableList.of())))));

    SortedMap<Flow, List<Trace>> deltaFlowTraces =
        ImmutableSortedMap.of(
            flow,
            ImmutableList.of(
                new Trace(FlowDisposition.ACCEPTED, ImmutableList.of()),
                new Trace(FlowDisposition.ACCEPTED, ImmutableList.of())));

    Multiset<Row> rows = diffFlowTracesToRows(baseflowTraces, deltaFlowTraces, 1);

    assertThat(
        rows.iterator().next(),
        allOf(
            hasColumn(
                COL_FLOW,
                allOf(hasDstIp(Ip.parse("1.1.1.1")), hasIngressNode("node"), hasTag("tag")),
                Schema.FLOW),
            hasColumn(COL_BASE_TRACES, hasSize(1), Schema.set(Schema.TRACE)),
            hasColumn(TracerouteAnswerer.COL_BASE_TRACE_COUNT, equalTo(2), Schema.INTEGER),
            hasColumn(COL_DELTA_TRACES, hasSize(1), Schema.set(Schema.TRACE)),
            hasColumn(TracerouteAnswerer.COL_DELTA_TRACE_COUNT, equalTo(2), Schema.INTEGER)));
  }

  @Test
  public void testMetadata() {
    List<ColumnMetadata> columnMetadata = metadata(false).getColumnMetadata();
    assertThat(
        columnMetadata.stream()
            .map(ColumnMetadata::getName)
            .collect(ImmutableList.toImmutableList()),
        contains(COL_FLOW, COL_TRACES, COL_TRACE_COUNT));

    assertThat(
        columnMetadata.stream()
            .map(ColumnMetadata::getSchema)
            .collect(ImmutableList.toImmutableList()),
        contains(Schema.FLOW, Schema.set(Schema.TRACE), Schema.INTEGER));

    List<ColumnMetadata> diffColumnMetadata = metadata(true).getColumnMetadata();
    assertThat(
        diffColumnMetadata.stream()
            .map(ColumnMetadata::getName)
            .collect(ImmutableList.toImmutableList()),
        contains(
            COL_FLOW,
            TracerouteAnswerer.COL_BASE_TRACES,
            TracerouteAnswerer.COL_BASE_TRACE_COUNT,
            TracerouteAnswerer.COL_DELTA_TRACES,
            TracerouteAnswerer.COL_DELTA_TRACE_COUNT));

    assertThat(
        diffColumnMetadata.stream()
            .map(ColumnMetadata::getSchema)
            .collect(ImmutableList.toImmutableList()),
        contains(
            Schema.FLOW,
            Schema.set(Schema.TRACE),
            Schema.INTEGER,
            Schema.set(Schema.TRACE),
            Schema.INTEGER));
  }

  @Test
  public void flowHistoryToRow() {
    Set<FlowTrace> traces =
        ImmutableSet.of(
            new FlowTrace(FlowDisposition.ACCEPTED, ImmutableList.of(), "notes1"),
            new FlowTrace(FlowDisposition.DENIED_OUT, ImmutableList.of(), "notes2"));
    Flow flow =
        Flow.builder().setTag("tag").setIngressNode("node").setDstIp(Ip.parse("1.1.1.1")).build();

    FlowHistoryInfo historyInfo =
        new FlowHistoryInfo(
            flow,
            ImmutableMap.of("env", new Environment(null, null, null, null, null, null, null)),
            ImmutableMap.of("env", traces));

    Row row = TracerouteAnswerer.flowHistoryToRow(historyInfo);

    assertThat(
        row,
        equalTo(
            Row.of(
                TracerouteAnswerer.COL_FLOW,
                flow,
                TracerouteAnswerer.COL_TRACES,
                historyInfo.getPaths().values().stream().findAny())));
  }
}

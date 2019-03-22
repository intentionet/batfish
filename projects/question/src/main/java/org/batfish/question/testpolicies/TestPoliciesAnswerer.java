package org.batfish.question.testpolicies;

import static org.batfish.datamodel.LineAction.DENY;
import static org.batfish.datamodel.LineAction.PERMIT;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import java.util.List;
import java.util.Map;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;

/** An answerer for {@link TestPoliciesQuestion}. */
public final class TestPoliciesAnswerer extends Answerer {
  public static final String COL_NODE = "Node";
  public static final String COL_POLICY_NAME = "Policy_Name";
  public static final String COL_INPUT_ROUTE = "Input_Route";
  public static final String COL_ACTION = "Action";
  public static final String COL_OUTPUT_ROUTE = "Output_Route";
  public static final String COL_DIFF = "Difference";

  private final Direction _direction;
  private final BgpRoute _inputRoute;
  private final String _node;
  private final String _policy;

  public TestPoliciesAnswerer(TestPoliciesQuestion question, IBatfish batfish) {
    super(question, batfish);
    _direction = question.getDirection();
    _inputRoute = question.getInputRoute();
    _node = question.getNodes();
    _policy = question.getPolicies();
  }

  @Override
  public AnswerElement answer() {
    Map<String, Configuration> configs = _batfish.loadConfigurations();

    RoutingPolicy policy = configs.get(_node).getRoutingPolicies().get(_policy);

    BgpRoute inputRoute = _inputRoute;
    BgpRoute.Builder outputRoute = inputRoute.toBuilder();

    boolean permit =
        policy.process(
            inputRoute, outputRoute, null, null, Configuration.DEFAULT_VRF_NAME, _direction);

    Row row = row(_node, _policy, inputRoute, permit ? PERMIT : DENY, outputRoute.build());
    Multiset<Row> rows = ImmutableMultiset.of(row);

    TableAnswerElement answerElement = new TableAnswerElement(metadata());
    answerElement.postProcessAnswer(_question, rows);
    return answerElement;
  }

  public static TableMetadata metadata() {
    List<ColumnMetadata> columnMetadata =
        ImmutableList.of(
            new ColumnMetadata(COL_NODE, Schema.NODE, "The node that has the policy", true, false),
            new ColumnMetadata(
                COL_POLICY_NAME, Schema.STRING, "The name of this policy", true, false),
            new ColumnMetadata(COL_INPUT_ROUTE, Schema.OBJECT, "The input route", true, false),
            new ColumnMetadata(
                COL_ACTION,
                Schema.STRING,
                "The action of the policy on the input route",
                false,
                true),
            new ColumnMetadata(
                COL_OUTPUT_ROUTE, Schema.OBJECT, "The output route, if any", false, true),
            new ColumnMetadata(
                COL_DIFF,
                Schema.list(Schema.OBJECT),
                "The difference between the input and output routes, if any",
                false,
                true));
    return new TableMetadata(
        columnMetadata, String.format("Results for route ${%s}", COL_INPUT_ROUTE));
  }

  private static Row row(
      String node, String policy, BgpRoute inputRoute, LineAction action, BgpRoute outputRoute) {
    boolean permit = action == PERMIT;
    return Row.builder()
        .put(COL_NODE, new Node(node))
        .put(COL_POLICY_NAME, policy)
        .put(COL_INPUT_ROUTE, inputRoute)
        .put(COL_ACTION, action)
        .put(COL_OUTPUT_ROUTE, permit ? outputRoute : null)
        .put(COL_DIFF, permit ? BgpRouteDiff.routeDiffs(inputRoute, outputRoute) : null)
        .build();
  }
}

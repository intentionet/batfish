package org.batfish.datamodel.flow;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nullable;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Route;

/**
 * {@link Step} to represent the selection of the outgoing {@link Interface} on a node for a {@link
 * org.batfish.datamodel.Flow}
 */
public class RoutingStep extends Step {

  /**
   * {@link StepDetail} containing details of routing to direct a {@link Flow} from an input {@link
   * Interface} to output {@link Interface}
   */
  public static class RoutingStepDetail extends StepDetail {

    private static final String PROP_ROUTES = "routes";

    /**
     * Information about {@link Route}s which led to the selection of the out {@link Interface}, can
     * be multiple in case of ECMP
     */
    private List<RouteInfo> _routes;

    @JsonCreator
    public RoutingStepDetail(
        @JsonProperty(PROP_ROUTES) @Nullable List<RouteInfo> routes,
        @JsonProperty(PROP_NAME) @Nullable String name) {
      super(firstNonNull(name, "Routing"));
      _routes = firstNonNull(routes, ImmutableList.of());
    }

    @JsonProperty(PROP_ROUTES)
    public List<RouteInfo> getRoutes() {
      return _routes;
    }

    public static Builder builder() {
      return new Builder();
    }

    /** Chained builder to create a {@link RoutingStepDetail} object */
    public static class Builder {
      private List<RouteInfo> _routes;
      private String _name;

      public RoutingStepDetail build() {
        return new RoutingStepDetail(_routes, _name);
      }

      public Builder setName(String name) {
        _name = name;
        return this;
      }

      public Builder setRoutes(List<RouteInfo> routes) {
        _routes = routes;
        return this;
      }
    }
  }

  public static class RoutingStepAction extends StepAction {

    private static final String PROP_ACTION_RESULT = "actionResult";

    private @Nullable StepActionResult _actionResult;

    @JsonCreator
    public RoutingStepAction(
        @JsonProperty(PROP_ACTION_RESULT) @Nullable StepActionResult result,
        @JsonProperty(PROP_NAME) @Nullable String name) {
      super(firstNonNull(name, "Routing"));
      _actionResult = result;
    }

    @Nullable
    @JsonProperty(PROP_ACTION_RESULT)
    public StepActionResult getActionResult() {
      return _actionResult;
    }
  }

  private static final String PROP_DETAIL = "detail";
  private static final String PROP_ACTION = "action";

  @JsonCreator
  public RoutingStep(
      @JsonProperty(PROP_DETAIL) RoutingStepDetail stepDetail,
      @JsonProperty(PROP_ACTION) RoutingStepAction stepAction) {
    super(stepDetail, stepAction);
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Chained builder to create a {@link RoutingStep} object */
  public static class Builder {
    private RoutingStepDetail _detail;
    private RoutingStepAction _action;

    public RoutingStep build() {
      return new RoutingStep(_detail, _action);
    }

    public Builder setDetail(RoutingStepDetail detail) {
      _detail = detail;
      return this;
    }

    public Builder setAction(RoutingStepAction action) {
      _action = action;
      return this;
    }
  }
}

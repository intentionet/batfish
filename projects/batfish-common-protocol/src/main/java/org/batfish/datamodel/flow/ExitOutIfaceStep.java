package org.batfish.datamodel.flow;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nullable;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.collections.NodeInterfacePair;

/**
 * {@link Step} to represent the exiting of a {@link Flow} from an outgoing {@link
 * org.batfish.datamodel.Interface} on a node
 */
public class ExitOutIfaceStep extends Step {

  private static final long serialVersionUID = 1L;

  /**
   * {@link StepDetail} containing details of the exiting of a {@link Flow} from an output {@link
   * org.batfish.datamodel.Interface}
   */
  public static class ExitOutIfaceStepDetail extends StepDetail {
    private static final String PROP_OUTPUT_INTERFACE = "outputInterface";
    private static final String PROP_FILTER = "filter";
    private static final String PROP_ORIGINAL_FLOW = "originalFlow";
    private static final String PROP_TRANSFORMED_FLOW = "transformedFlow";

    private static final long serialVersionUID = 1L;

    private @Nullable NodeInterfacePair _outputInterface;
    private @Nullable String _filter;
    private @Nullable Flow _originalFlow;
    private @Nullable Flow _transformedFlow;

    private ExitOutIfaceStepDetail(
        @JsonProperty(PROP_OUTPUT_INTERFACE) @Nullable NodeInterfacePair outInterface,
        @JsonProperty(PROP_FILTER) @Nullable String filter,
        @JsonProperty(PROP_ORIGINAL_FLOW) @Nullable Flow originalFlow,
        @JsonProperty(PROP_TRANSFORMED_FLOW) @Nullable() Flow transformedFlow,
        @JsonProperty(PROP_NAME) @Nullable String name) {
      super(firstNonNull(name, "ExitOutIface"));
      _outputInterface = outInterface;
      _filter = filter;
      _originalFlow = originalFlow;
      _transformedFlow = transformedFlow;
    }

    @JsonProperty(PROP_OUTPUT_INTERFACE)
    @Nullable
    public NodeInterfacePair getOutputInterface() {
      return _outputInterface;
    }

    @JsonProperty(PROP_FILTER)
    @Nullable
    public String getFilterIOut() {
      return _filter;
    }

    @JsonProperty(PROP_ORIGINAL_FLOW)
    @Nullable
    public Flow getOriginalFlow() {
      return _originalFlow;
    }

    @JsonProperty(PROP_TRANSFORMED_FLOW)
    @Nullable
    public Flow getTransformedFlow() {
      return _transformedFlow;
    }

    public static Builder builder() {
      return new Builder();
    }

    /** Chained builder to create a {@link ExitOutIfaceStepDetail} object */
    public static class Builder {
      private NodeInterfacePair _outputInterface;
      private String _filter;
      private Flow _originalFlow;
      private Flow _transformedFlow;
      private String _name;

      public ExitOutIfaceStepDetail build() {
        return new ExitOutIfaceStepDetail(
            _outputInterface, _filter, _originalFlow, _transformedFlow, _name);
      }

      public Builder setName(String name) {
        _name = name;
        return this;
      }

      public Builder setOutputInterface(NodeInterfacePair outputIface) {
        _outputInterface = outputIface;
        return this;
      }

      public Builder setFilter(String filter) {
        _filter = filter;
        return this;
      }

      public Builder setOriginalFlow(Flow originalFlow) {
        _originalFlow = originalFlow;
        return this;
      }

      public Builder setTransformedFlow(Flow transformedFlow) {
        _transformedFlow = transformedFlow;
        return this;
      }
    }
  }

  public static class ExitOutIfaceAction extends StepAction {

    private static final String PROP_ACTION_RESULT = "actionResult";

    private static final long serialVersionUID = 1L;

    private @Nullable StepActionResult _actionResult;

    public ExitOutIfaceAction(
        @JsonProperty(PROP_ACTION_RESULT) @Nullable StepActionResult result,
        @JsonProperty(PROP_NAME) @Nullable String name) {
      super(firstNonNull(name, "ExitOutIface"));
      _actionResult = result;
    }

    @JsonProperty(PROP_ACTION_RESULT)
    @Nullable
    public StepActionResult getActionResult() {
      return _actionResult;
    }
  }

  private static final String PROP_DETAIL = "detail";
  private static final String PROP_ACTION = "action";

  @JsonCreator
  public ExitOutIfaceStep(
      @JsonProperty(PROP_DETAIL) ExitOutIfaceStepDetail stepDetail,
      @JsonProperty(PROP_ACTION) ExitOutIfaceAction stepAction) {
    super(stepDetail, stepAction);
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Chained builder to create a {@link ExitOutIfaceStep} object */
  public static class Builder {
    private ExitOutIfaceStepDetail _detail;
    private ExitOutIfaceAction _action;

    public ExitOutIfaceStep build() {
      return new ExitOutIfaceStep(_detail, _action);
    }

    public Builder setDetail(ExitOutIfaceStepDetail detail) {
      _detail = detail;
      return this;
    }

    public Builder setAction(ExitOutIfaceAction action) {
      _action = action;
      return this;
    }
  }
}

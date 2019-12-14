package org.batfish.datamodel.flow;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.flow.RoutingStep.RoutingStepDetail;

/**
 * {@link Step} to represent the selection of the outgoing {@link Interface} on a node for a {@link
 * org.batfish.datamodel.Flow}
 */
@JsonTypeName("Routing")
public final class RoutingStep extends Step<RoutingStepDetail> {

  /**
   * Details of {@link Step} about routing to direct a {@link Flow} from an input {@link Interface}
   * to output {@link Interface}
   */
  public static final class RoutingStepDetail {
    private static final String PROP_MATCHED_ROUTES = "matchedRoutes";
    private static final String PROP_FINAL_NEXT_HOP_IP = "finalNextHopIp";
    private static final String PROP_FINAL_NEXT_HOP_INTERFACE = "finalNextHopInterface";

    /**
     * Information about {@link Route}s which led to the selection of the out {@link Interface}, can
     * be multiple in case of ECMP
     */
    @Nonnull private final List<RouteInfo> _matchedRoutes;
    /** Next hop IP which was resolved using the {@code _matchedRoutes} */
    @Nullable private final Ip _finalNextHopIp;
    /** Next hop interface which was resolved using the {@code _matchedRoutes} */
    @Nullable private final String _finalNextHopInterface;

    @JsonCreator
    private RoutingStepDetail(
        @JsonProperty(PROP_MATCHED_ROUTES) @Nullable List<RouteInfo> matchedRoutes,
        @JsonProperty(PROP_FINAL_NEXT_HOP_IP) @Nullable Ip finalNextHopIp,
        @JsonProperty(PROP_FINAL_NEXT_HOP_INTERFACE) @Nullable String finalNextHopInterface) {
      _matchedRoutes = firstNonNull(matchedRoutes, ImmutableList.of());
      _finalNextHopIp = finalNextHopIp;
      _finalNextHopInterface = finalNextHopInterface;
    }

    @JsonProperty(PROP_MATCHED_ROUTES)
    @Nonnull
    public List<RouteInfo> getMatchedRoutes() {
      return _matchedRoutes;
    }

    @JsonProperty(PROP_FINAL_NEXT_HOP_IP)
    @Nullable
    public Ip getFinalNextHopIp() {
      return _finalNextHopIp;
    }

    @JsonProperty(PROP_FINAL_NEXT_HOP_INTERFACE)
    @Nullable
    public String getFinalNextHopInterface() {
      return _finalNextHopInterface;
    }

    public static Builder builder() {
      return new Builder();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof RoutingStepDetail)) {
        return false;
      }
      RoutingStepDetail that = (RoutingStepDetail) o;
      return _matchedRoutes.equals(that._matchedRoutes)
          && Objects.equals(_finalNextHopIp, that._finalNextHopIp)
          && Objects.equals(_finalNextHopInterface, that._finalNextHopInterface);
    }

    @Override
    public int hashCode() {
      return Objects.hash(_matchedRoutes, _finalNextHopIp, _finalNextHopInterface);
    }

    /** Chained builder to create a {@link RoutingStepDetail} object */
    public static class Builder {
      private @Nullable List<RouteInfo> _matchedRoutes;
      private @Nullable Ip _finalNextHopIp;
      private @Nullable String _finalNextHopInterface;

      public RoutingStepDetail build() {
        return new RoutingStepDetail(_matchedRoutes, _finalNextHopIp, _finalNextHopInterface);
      }

      public Builder setMatchedRoutes(List<RouteInfo> matchedRoutes) {
        _matchedRoutes = matchedRoutes;
        return this;
      }

      public Builder setFinalNextHopIp(@Nullable Ip finalNextHopIp) {
        _finalNextHopIp = finalNextHopIp;
        return this;
      }

      public Builder setFinalNextHopInterface(@Nullable String finalNextHopInterface) {
        _finalNextHopInterface = finalNextHopInterface;
        return this;
      }

      /** Only for use by {@link RoutingStepDetail#builder()}. */
      private Builder() {}
    }
  }

  private static final String PROP_DETAIL = "detail";
  private static final String PROP_ACTION = "action";

  @JsonCreator
  private static RoutingStep jsonCreator(
      @Nullable @JsonProperty(PROP_DETAIL) RoutingStepDetail detail,
      @Nullable @JsonProperty(PROP_ACTION) StepAction action) {
    checkArgument(action != null, "Missing %s", PROP_ACTION);
    checkArgument(detail != null, "Missing %s", PROP_DETAIL);
    return new RoutingStep(detail, action);
  }

  private RoutingStep(RoutingStepDetail detail, StepAction action) {
    super(detail, action);
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Chained builder to create a {@link RoutingStep} object */
  public static final class Builder {
    private @Nullable RoutingStepDetail _detail;
    private @Nullable StepAction _action;

    public RoutingStep build() {
      checkState(_action != null, "Must call setAction before building");
      checkState(_detail != null, "Must call setDetail before building");
      return new RoutingStep(_detail, _action);
    }

    public Builder setAction(StepAction action) {
      _action = action;
      return this;
    }

    public Builder setDetail(RoutingStepDetail detail) {
      _detail = detail;
      return this;
    }

    /** Only for use by {@link RoutingStep#builder()}. */
    private Builder() {}
  }
}

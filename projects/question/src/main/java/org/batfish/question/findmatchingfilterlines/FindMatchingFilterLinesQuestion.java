package org.batfish.question.findmatchingfilterlines;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.PacketHeaderConstraints;
import org.batfish.datamodel.questions.Question;
import org.batfish.specifier.AllFiltersFilterSpecifier;
import org.batfish.specifier.AllNodesNodeSpecifier;
import org.batfish.specifier.FilterSpecifier;
import org.batfish.specifier.NodeSpecifier;
import org.batfish.specifier.SpecifierFactories;

/** A question to find filter lines match packets in common with a given header constraint. */
public final class FindMatchingFilterLinesQuestion extends Question {
  private static final String PROP_ACTION = "action";
  private static final String PROP_FILTERS = "filters";
  private static final String PROP_HEADERS = "headers";
  private static final String PROP_IGNORE_COMPOSITES = "ignoreComposites";
  private static final String PROP_NODES = "nodes";

  private static final boolean DEFAULT_IGNORE_COMPOSITES = true;

  @Nullable private final LineAction _action;
  @Nullable private final String _filters;
  @Nonnull private final FilterSpecifier _filterSpecifier;
  @Nonnull private final PacketHeaderConstraints _headerConstraints;
  private final boolean _ignoreComposites;
  @Nullable private final String _nodes;
  @Nonnull private final NodeSpecifier _nodeSpecifier;

  @JsonCreator
  private static FindMatchingFilterLinesQuestion create(
      @JsonProperty(PROP_ACTION) @Nullable String action,
      @JsonProperty(PROP_FILTERS) @Nullable String filters,
      @JsonProperty(PROP_HEADERS) @Nullable PacketHeaderConstraints headerConstraints,
      @JsonProperty(PROP_IGNORE_COMPOSITES) @Nullable Boolean ignoreComposites,
      @JsonProperty(PROP_NODES) @Nullable String nodes) {
    LineAction lineAction = null;
    if (action != null) {
      if (action.equalsIgnoreCase("permit")) {
        lineAction = LineAction.PERMIT;
      } else if (action.equalsIgnoreCase("deny")) {
        lineAction = LineAction.DENY;
      }
      checkArgument(lineAction != null, "Unrecognized action: ", action);
    }
    return new FindMatchingFilterLinesQuestion(
        nodes, filters, lineAction, headerConstraints, ignoreComposites);
  }

  FindMatchingFilterLinesQuestion(
      @Nullable String nodes,
      @Nullable String filters,
      @Nullable LineAction action,
      @Nullable PacketHeaderConstraints headerConstraints,
      @Nullable Boolean ignoreComposites) {
    _action = action;
    _filters = filters;
    _filterSpecifier =
        SpecifierFactories.getFilterSpecifierOrDefault(filters, AllFiltersFilterSpecifier.INSTANCE);
    _headerConstraints = firstNonNull(headerConstraints, PacketHeaderConstraints.unconstrained());
    _ignoreComposites = firstNonNull(ignoreComposites, DEFAULT_IGNORE_COMPOSITES);
    _nodes = nodes;
    _nodeSpecifier =
        SpecifierFactories.getNodeSpecifierOrDefault(nodes, AllNodesNodeSpecifier.INSTANCE);
  }

  FindMatchingFilterLinesQuestion() {
    this(null, null, null, null, null);
  }

  @Override
  public boolean getDataPlane() {
    return false;
  }

  @Override
  public String getName() {
    return "findmatchingfilterlines";
  }

  @JsonProperty(PROP_ACTION)
  @Nullable
  public LineAction getAction() {
    return _action;
  }

  @Nullable
  @JsonProperty(PROP_FILTERS)
  private String getFilters() {
    return _filters;
  }

  @Nonnull
  @JsonIgnore
  FilterSpecifier getFilterSpecifier() {
    return _filterSpecifier;
  }

  @JsonProperty(PROP_HEADERS)
  @Nonnull
  public PacketHeaderConstraints getHeaderConstraints() {
    return _headerConstraints;
  }

  @JsonProperty(PROP_IGNORE_COMPOSITES)
  public boolean getIgnoreComposites() {
    return _ignoreComposites;
  }

  @JsonProperty(PROP_NODES)
  @Nullable
  private String getNodes() {
    return _nodes;
  }

  @Nonnull
  @JsonIgnore
  NodeSpecifier getNodeSpecifier() {
    return _nodeSpecifier;
  }
}

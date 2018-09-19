package org.batfish.question.testfilters;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.PacketHeaderConstraints;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.specifier.FilterSpecifier;
import org.batfish.specifier.FilterSpecifierFactory;
import org.batfish.specifier.FlexibleFilterSpecifierFactory;
import org.batfish.specifier.FlexibleLocationSpecifierFactory;
import org.batfish.specifier.LocationSpecifier;
import org.batfish.specifier.LocationSpecifierFactory;

/**
 * Computes the fate of the flow at a filter. The set of filters to consider are controlled by
 * 'nodes' and 'filters' fields. By default, all filters on all nodes are considered.
 */
public class TestFiltersQuestion extends Question {

  private static final String FILTER_SPECIFIER_FACTORY = FlexibleFilterSpecifierFactory.NAME;
  private static final String LOCATION_SPECIFIER_FACTORY = FlexibleLocationSpecifierFactory.NAME;

  private static final String PROP_FILTERS = "filters";
  private static final String PROP_HEADERS = "headers";
  private static final String PROP_NODES = "nodes";
  private static final String PROP_START_LOCATION = "startLocation";

  @Nullable private final String _filters;
  @Nonnull private final PacketHeaderConstraints _headers;
  @Nonnull private final NodesSpecifier _nodes;
  @Nullable private final String _startLocation;

  @JsonCreator
  public TestFiltersQuestion(
      @JsonProperty(PROP_NODES) NodesSpecifier nodes,
      @JsonProperty(PROP_FILTERS) String filters,
      @JsonProperty(PROP_HEADERS) PacketHeaderConstraints headers,
      @JsonProperty(PROP_START_LOCATION) String startLocation) {
    _nodes = nodes == null ? NodesSpecifier.ALL : nodes;
    _filters = filters;
    _headers = firstNonNull(headers, PacketHeaderConstraints.unconstrained());
    _startLocation = startLocation;
  }

  @Override
  public boolean getDataPlane() {
    return false;
  }

  @JsonProperty(PROP_FILTERS)
  private String getFilters() {
    return _filters;
  }

  @Nonnull
  @JsonIgnore
  public FilterSpecifier getFilterSpecifier() {
    return FilterSpecifierFactory.load(FILTER_SPECIFIER_FACTORY).buildFilterSpecifier(_filters);
  }

  @Nonnull
  @JsonProperty(PROP_HEADERS)
  public PacketHeaderConstraints getHeaders() {
    return _headers;
  }

  @Override
  public String getName() {
    return "testFilters";
  }

  @JsonProperty(PROP_NODES)
  public NodesSpecifier getNodes() {
    return _nodes;
  }

  @JsonProperty(PROP_START_LOCATION)
  public String getStartLocation() {
    return _startLocation;
  }

  @Nonnull
  @JsonIgnore
  public LocationSpecifier getStartLocationSpecifier() {
    return LocationSpecifierFactory.load(LOCATION_SPECIFIER_FACTORY)
        .buildLocationSpecifier(_startLocation);
  }
}

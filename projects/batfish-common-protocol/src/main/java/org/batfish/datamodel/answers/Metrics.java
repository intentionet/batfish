package org.batfish.datamodel.answers;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.BfConsts;

public class Metrics {

  public static class Builder {
    private Map<String, Map<Aggregation, Object>> _aggregations;

    private Set<String> _emptyColumns;

    private Map<String, MajorIssueConfig> _majorIssueConfigs;

    private Integer _numRows;

    private Builder() {
      _aggregations = ImmutableMap.of();
      _emptyColumns = ImmutableSet.of();
      _majorIssueConfigs = ImmutableMap.of();
    }

    public @Nonnull Metrics build() {
      return new Metrics(
          _aggregations, _emptyColumns, _majorIssueConfigs, requireNonNull(_numRows));
    }

    public @Nonnull Builder setAggregations(
        @Nonnull Map<String, Map<Aggregation, Object>> aggregations) {
      _aggregations = ImmutableMap.copyOf(aggregations);
      return this;
    }

    public @Nonnull Builder setEmptyColumns(@Nonnull Set<String> emptyColumns) {
      _emptyColumns = ImmutableSet.copyOf(emptyColumns);
      return this;
    }

    public @Nonnull Builder setMajorIssueConfigs(
        @Nonnull Map<String, MajorIssueConfig> majorIssueConfigs) {
      _majorIssueConfigs = ImmutableMap.copyOf(majorIssueConfigs);
      return this;
    }

    public @Nonnull Builder setNumRows(int numRows) {
      _numRows = numRows;
      return this;
    }
  }

  public static @Nonnull Builder builder() {
    return new Builder();
  }

  @JsonCreator
  private static @Nonnull Metrics create(
      @JsonProperty(BfConsts.PROP_AGGREGATIONS) Map<String, Map<Aggregation, Object>> aggregations,
      @JsonProperty(BfConsts.PROP_EMPTY_COLUMNS) Set<String> emptyColumns,
      @JsonProperty(BfConsts.PROP_MAJOR_ISSUE_CONFIGS)
          Map<String, MajorIssueConfig> majorIssueConfigs,
      @JsonProperty(BfConsts.PROP_NUM_ROWS) int numRows) {
    return new Metrics(
        firstNonNull(aggregations, ImmutableMap.of()),
        firstNonNull(emptyColumns, ImmutableSet.of()),
        firstNonNull(majorIssueConfigs, ImmutableMap.of()),
        numRows);
  }

  private final Map<String, Map<Aggregation, Object>> _aggregations;

  private final Set<String> _emptyColumns;

  private final Map<String, MajorIssueConfig> _majorIssueConfigs;

  private final int _numRows;

  private Metrics(
      @Nonnull Map<String, Map<Aggregation, Object>> aggregations,
      @Nonnull Set<String> emptyColumns,
      @Nonnull Map<String, MajorIssueConfig> majorIssueConfigs,
      int numRows) {
    _aggregations = aggregations;
    _emptyColumns = emptyColumns;
    _majorIssueConfigs = majorIssueConfigs;
    _numRows = numRows;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Metrics)) {
      return false;
    }
    Metrics rhs = (Metrics) obj;
    return _aggregations.equals(rhs._aggregations)
        && _emptyColumns.equals(rhs._emptyColumns)
        && _majorIssueConfigs.equals(rhs._majorIssueConfigs)
        && _numRows == rhs._numRows;
  }

  @JsonProperty(BfConsts.PROP_AGGREGATIONS)
  public @Nonnull Map<String, Map<Aggregation, Object>> getAggregations() {
    return _aggregations;
  }

  @JsonProperty(BfConsts.PROP_EMPTY_COLUMNS)
  public Set<String> getEmptyColumns() {
    return _emptyColumns;
  }

  @JsonProperty(BfConsts.PROP_MAJOR_ISSUE_CONFIGS)
  public Map<String, MajorIssueConfig> getMajorIssueConfigs() {
    return _majorIssueConfigs;
  }

  @JsonProperty(BfConsts.PROP_NUM_ROWS)
  public int getNumRows() {
    return _numRows;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_aggregations, _emptyColumns, _majorIssueConfigs, _numRows);
  }

  @Override
  public String toString() {
    return toStringHelper(getClass())
        .omitNullValues()
        .add(BfConsts.PROP_AGGREGATIONS, _aggregations)
        .add(BfConsts.PROP_EMPTY_COLUMNS, _emptyColumns)
        .add(
            BfConsts.PROP_MAJOR_ISSUE_CONFIGS,
            !_majorIssueConfigs.isEmpty() ? _majorIssueConfigs : null)
        .add(BfConsts.PROP_NUM_ROWS, _numRows)
        .toString();
  }
}

package org.batfish.datamodel.answers;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/** Represents the configuration of a major issue type */
@ParametersAreNonnullByDefault
public class MajorIssueConfig {

  private static final String PROP_MAJOR_ISSUE = "majorIssue";
  private static final String PROP_MINOR_ISSUE_CONFIGS = "minorIssueConfigs";

  @Nonnull private final String _majorIssue;
  @Nonnull private Map<String, MinorIssueConfig> _minorIssueConfigs;

  @JsonCreator
  private static @Nonnull MajorIssueConfig create(
      @JsonProperty(PROP_MAJOR_ISSUE) @Nullable String majorIssue,
      @JsonProperty(PROP_MINOR_ISSUE_CONFIGS) @Nullable List<MinorIssueConfig> minorIssueConfigs) {
    return new MajorIssueConfig(
        requireNonNull(majorIssue, "'majorIssue' cannot be null"),
        firstNonNull(minorIssueConfigs, ImmutableList.of()));
  }

  public MajorIssueConfig(String majorIssue, List<MinorIssueConfig> minorIssueConfigs) {
    ImmutableMap.Builder<String, MinorIssueConfig> minorIssueConfigsMap = ImmutableMap.builder();
    if (minorIssueConfigs != null) {
      minorIssueConfigs.forEach(
          minorIssueConfig ->
              minorIssueConfigsMap.put(minorIssueConfig.getMinor(), minorIssueConfig));
    }
    _majorIssue = majorIssue;
    _minorIssueConfigs = minorIssueConfigsMap.build();
  }

  public MajorIssueConfig(String majorIssue, Map<String, MinorIssueConfig> minorIssueConfigs) {
    _majorIssue = majorIssue;
    _minorIssueConfigs = minorIssueConfigs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MajorIssueConfig)) {
      return false;
    }
    MajorIssueConfig rhs = (MajorIssueConfig) o;
    return _majorIssue.equals(rhs._majorIssue) && _minorIssueConfigs.equals(rhs._minorIssueConfigs);
  }

  /** Returns a new MajorIssueConfig without the specified {@code minorIssue} */
  public MajorIssueConfig delMinorIssueConfig(String minorIssue) {
    Map<String, MinorIssueConfig> newMap = new HashMap<>(_minorIssueConfigs);
    newMap.remove(minorIssue);
    return new MajorIssueConfig(_majorIssue, newMap);
  }

  @JsonProperty(PROP_MAJOR_ISSUE)
  @Nonnull
  public String getMajorIssue() {
    return _majorIssue;
  }

  /** Returns the MinorIssueConfig for the specified {@code minorIssue} */
  public Optional<MinorIssueConfig> getMinorIssueConfig(String minorIssue) {
    return Optional.ofNullable(_minorIssueConfigs.get(minorIssue));
  }

  @JsonIgnore
  @Nonnull
  public Map<String, MinorIssueConfig> getMinorIssueConfigsMap() {
    return _minorIssueConfigs;
  }

  @JsonProperty(PROP_MINOR_ISSUE_CONFIGS)
  public List<MinorIssueConfig> getMinorIssueConfigs() {
    return _minorIssueConfigs.values().stream().collect(ImmutableList.toImmutableList());
  }

  @Override
  public int hashCode() {
    return Objects.hash(_majorIssue, _minorIssueConfigs);
  }

  /** Returns a new MajorIssueConfig with the minorIssueConfig inserted. */
  public MajorIssueConfig put(MinorIssueConfig minorIssueConfig) {
    Map<String, MinorIssueConfig> newMap = new HashMap<>(_minorIssueConfigs);
    newMap.put(minorIssueConfig.getMinor(), minorIssueConfig);
    return new MajorIssueConfig(_majorIssue, newMap);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(MajorIssueConfig.class)
        .add(PROP_MAJOR_ISSUE, _majorIssue)
        .add(PROP_MINOR_ISSUE_CONFIGS, _minorIssueConfigs)
        .toString();
  }
}

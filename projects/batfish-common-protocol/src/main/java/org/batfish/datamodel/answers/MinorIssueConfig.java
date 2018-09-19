package org.batfish.datamodel.answers;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/** Represents the configuration of a minor issue type */
@ParametersAreNonnullByDefault
public class MinorIssueConfig {
  private static final String PROP_MINOR_ISSUE = "minorIssue";
  private static final String PROP_SEVERITY = "severity";
  private static final String PROP_URL = "url";

  @Nonnull private final String _minorIssue;
  @Nullable private final Integer _severity;
  @Nullable private final String _url;

  @JsonCreator
  private static @Nonnull MinorIssueConfig create(
      @JsonProperty(PROP_MINOR_ISSUE) @Nullable String minorIssue,
      @JsonProperty(PROP_SEVERITY) @Nullable Integer severity,
      @JsonProperty(PROP_URL) @Nullable String url) {
    return new MinorIssueConfig(
        requireNonNull(minorIssue, "'minorIssue' cannot be null"), severity, url);
  }

  public MinorIssueConfig(String minorIssue, Integer severity, String url) {
    _minorIssue = minorIssue;
    _severity = severity;
    _url = url;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MinorIssueConfig)) {
      return false;
    }
    MinorIssueConfig rhs = (MinorIssueConfig) o;
    return _minorIssue.equals(rhs._minorIssue)
        && Objects.equals(_severity, rhs._severity)
        && Objects.equals(_url, rhs._url);
  }

  @JsonProperty(PROP_MINOR_ISSUE)
  @Nonnull
  public String getMinor() {
    return _minorIssue;
  }

  @JsonProperty(PROP_SEVERITY)
  @Nullable
  public Integer getSeverity() {
    return _severity;
  }

  @JsonProperty(PROP_URL)
  @Nullable
  public String getUrl() {
    return _url;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_minorIssue, _severity, _url);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(MinorIssueConfig.class)
        .add(PROP_MINOR_ISSUE, _minorIssue)
        .add(PROP_SEVERITY, _severity)
        .add(PROP_URL, _url)
        .toString();
  }
}

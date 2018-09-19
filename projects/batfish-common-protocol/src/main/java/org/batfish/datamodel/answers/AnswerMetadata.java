package org.batfish.datamodel.answers;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.BfConsts;

@ParametersAreNonnullByDefault
public class AnswerMetadata {

  public static class Builder {
    private Metrics _metrics;

    private AnswerStatus _status;

    public @Nonnull AnswerMetadata build() {
      return new AnswerMetadata(_metrics, requireNonNull(_status));
    }

    public @Nonnull Builder setMetrics(@Nullable Metrics metrics) {
      _metrics = metrics;
      return this;
    }

    public @Nonnull Builder setStatus(@Nonnull AnswerStatus status) {
      _status = status;
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  @JsonCreator
  private static @Nonnull AnswerMetadata create(
      @JsonProperty(BfConsts.PROP_METRICS) @Nullable Metrics metrics,
      @JsonProperty(BfConsts.PROP_STATUS) @Nullable AnswerStatus status) {
    return new AnswerMetadata(metrics, requireNonNull(status));
  }

  public static @Nonnull AnswerMetadata forStatus(AnswerStatus status) {
    return new Builder().setStatus(status).build();
  }

  private final Metrics _metrics;

  private final AnswerStatus _status;

  private AnswerMetadata(@Nullable Metrics metrics, @Nonnull AnswerStatus status) {
    _metrics = metrics;
    _status = status;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof AnswerMetadata)) {
      return false;
    }
    AnswerMetadata rhs = (AnswerMetadata) obj;
    return Objects.equals(_metrics, rhs._metrics) && _status == rhs._status;
  }

  @JsonProperty(BfConsts.PROP_METRICS)
  public @Nullable Metrics getMetrics() {
    return _metrics;
  }

  @JsonProperty(BfConsts.PROP_STATUS)
  public @Nonnull AnswerStatus getStatus() {
    return _status;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_metrics, _status.ordinal());
  }

  @Override
  public String toString() {
    return toStringHelper(getClass())
        .add(BfConsts.PROP_METRICS, _metrics)
        .add(BfConsts.PROP_STATUS, _status)
        .toString();
  }
}

package org.batfish.common.util;

import java.time.Duration;
import java.util.NoSuchElementException;

/** Provides a simple interface for an implementation of various types of temporal backoff. */
public abstract class Backoff {
  /**
   * Returns {@code true} if this {@link Backoff} is able to continue, i.e., the number of attempts
   * has not been exceeded and the maximum cumulative backoff time has not been reached.
   */
  public abstract boolean hasNext();

  /**
   * Returns a {@link Duration} representing the next amount of time to back off. If this {@link
   * Backoff} is not able to continue, i.e. {@link #hasNext()} would return {@code false}, throws
   * {@link java.util.NoSuchElementException}.
   */
  public abstract Duration nextBackoff() throws NoSuchElementException;

  public static final Duration DEFAULT_INITIAL_BACKOFF = Duration.ofMillis(1);
  public static final Duration DEFAULT_MAXIMUM_BACKOFF = Duration.ofMinutes(1);
  public static final Duration DEFAULT_CUMULATIVE_BACKOFF_LIMIT = Duration.ofDays(365);
  public static final int DEFAULT_ATTEMPT_LIMIT = Integer.MAX_VALUE;
  public static final double DEFAULT_EXPANSION = 1.5;

  public static Builder builder() {
    return new Builder(
        DEFAULT_INITIAL_BACKOFF,
        DEFAULT_MAXIMUM_BACKOFF,
        DEFAULT_CUMULATIVE_BACKOFF_LIMIT,
        DEFAULT_ATTEMPT_LIMIT,
        DEFAULT_EXPANSION);
  }

  public static class Builder {
    private final Duration _initialBackoff;
    private final Duration _maximumBackoff;
    private final Duration _cumulativeBackoffLimit;
    private final int _attemptLimit;
    private final double _expansion;

    public Backoff build() {
      return new BackoffImpl(this);
    }

    // Prevent instantiation
    private Builder(
        Duration initialBackoff,
        Duration maximumBackoff,
        Duration cumulativeBackoffLimit,
        int attempts,
        double expansion) {
      this._initialBackoff = initialBackoff;
      this._maximumBackoff = maximumBackoff;
      this._cumulativeBackoffLimit = cumulativeBackoffLimit;
      this._attemptLimit = attempts;
      this._expansion = expansion;
    }

    public Builder withInitialBackoff(Duration initialBackoff) {
      return new Builder(
          initialBackoff, _maximumBackoff, _cumulativeBackoffLimit, _attemptLimit, _expansion);
    }

    public Builder withMaximumBackoff(Duration maximumBackoff) {
      return new Builder(
          _initialBackoff, maximumBackoff, _cumulativeBackoffLimit, _attemptLimit, _expansion);
    }

    public Builder withCumulativeBackoffLimit(Duration cumulativeBackoffLimit) {
      return new Builder(
          _initialBackoff, _maximumBackoff, cumulativeBackoffLimit, _attemptLimit, _expansion);
    }

    public Builder withAttemptLimit(int attemptLimit) {
      return new Builder(
          _initialBackoff, _maximumBackoff, _cumulativeBackoffLimit, attemptLimit, _expansion);
    }

    public Builder withExpansion(double expansion) {
      return new Builder(
          _initialBackoff, _maximumBackoff, _cumulativeBackoffLimit, _attemptLimit, expansion);
    }
  }

  // Only visible for testing.
  static class BackoffImpl extends Backoff {
    private Builder _builder;
    private Duration _nextBackoff;
    private Duration _cumulativeBackoff;
    private int _attempts;

    private BackoffImpl(Builder builder) {
      this._builder = builder;
      _nextBackoff = builder._initialBackoff;
      _cumulativeBackoff = Duration.ZERO;
      _attempts = 0;
    }

    @Override
    public boolean hasNext() {
      return (_attempts + 1) < _builder._attemptLimit
          && _cumulativeBackoff.compareTo(_builder._cumulativeBackoffLimit) < 0;
    }

    @Override
    public Duration nextBackoff() {
      if (!hasNext()) {
        throw new NoSuchElementException("Not able to continue backing off");
      }

      // By calling nextBackoff, the caller is recording a prior attempt.
      ++_attempts;

      // Save the current backoff to be returned.
      Duration ret = _nextBackoff;
      _cumulativeBackoff = _cumulativeBackoff.plus(ret);

      // Scale up the next backoff.
      Duration nextDuration =
          Duration.ofMillis((long) (0.5 + _nextBackoff.toMillis() * _builder._expansion));

      // Limit it by the maximum total backoff.
      _nextBackoff =
          nextDuration.compareTo(_builder._maximumBackoff) < 0
              ? nextDuration
              : _builder._maximumBackoff;

      return ret;
    }
  }
}

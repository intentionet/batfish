package org.batfish.question.reachfilter;

import java.util.Optional;
import javax.annotation.Nullable;

/** The result of a differential reachFilter question. */
public final class DifferentialReachFilterResult {
  private final @Nullable ReachFilterResult _decreasedResult;
  private final @Nullable ReachFilterResult _increasedResult;

  public DifferentialReachFilterResult(
      @Nullable ReachFilterResult increasedResult, @Nullable ReachFilterResult decreasedResult) {
    _decreasedResult = decreasedResult;
    _increasedResult = increasedResult;
  }

  /** @return The result for the decreased space. */
  public Optional<ReachFilterResult> getDecreasedResult() {
    return Optional.ofNullable(_decreasedResult);
  }

  /** @return The result for the decreased space. */
  public Optional<ReachFilterResult> getIncreasedResult() {
    return Optional.ofNullable(_increasedResult);
  }
}

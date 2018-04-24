package org.batfish.datamodel.matchers;

import static org.hamcrest.Matchers.equalTo;

import org.batfish.datamodel.matchers.BgpNeighborMatchersImpl.HasEnforceFirstAs;
import org.batfish.datamodel.matchers.BgpNeighborMatchersImpl.HasLocalAs;
import org.batfish.datamodel.matchers.BgpNeighborMatchersImpl.IsDynamic;
import org.hamcrest.Matcher;

public class BgpNeighborMatchers {

  /** Provides a matcher that matches if the BGP neighbor has the specified localAs. */
  public static HasLocalAs hasLocalAs(Integer localAs) {
    return new HasLocalAs(equalTo(localAs));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the BGP neighbor's
   * localAs.
   */
  public static HasLocalAs hasLocalAs(Matcher<? super Integer> subMatcher) {
    return new HasLocalAs(subMatcher);
  }

  /**
   * Provides a matcher that matches if the BGP neighbor's value of {@code enforceFirstAs} matches
   * {@code subMatcher}
   */
  public static HasEnforceFirstAs hasEnforceFirstAs(Matcher<? super Boolean> subMatcher) {
    return new HasEnforceFirstAs(subMatcher);
  }

  /**
   * Provides a matcher that matches if the BGP neighbor's value of {@code enforceFirstAs} is {@code
   * true}.
   */
  public static HasEnforceFirstAs hasEnforceFirstAs() {
    return new HasEnforceFirstAs(equalTo(true));
  }

  /**
   * Provides a matcher that matches if the BGP neighbor is configured as a listening end of a
   * dynamic BGP peering.
   */
  public static IsDynamic isDynamic() {
    return new IsDynamic(equalTo(true));
  }
}

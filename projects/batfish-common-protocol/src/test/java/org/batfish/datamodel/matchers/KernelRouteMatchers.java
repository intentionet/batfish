package org.batfish.datamodel.matchers;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.KernelRoute;
import org.batfish.datamodel.matchers.KernelRouteMatchersImpl.IsKernelRouteThat;
import org.hamcrest.Matcher;

@ParametersAreNonnullByDefault
public final class KernelRouteMatchers {

  /**
   * Provides a matcher that matches when the {@link AbstractRoute} is a {@link KernelRoute} matched
   * by the provided {@link subMatcher}.
   */
  public static @Nonnull Matcher<AbstractRoute> isKernelRouteThat(
      Matcher<? super KernelRoute> subMatcher) {
    return new IsKernelRouteThat(subMatcher);
  }

  private KernelRouteMatchers() {}
}

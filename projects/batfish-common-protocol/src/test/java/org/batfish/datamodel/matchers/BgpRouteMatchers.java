package org.batfish.datamodel.matchers;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.matchers.BgpRouteMatchersImpl.HasCommunities;
import org.batfish.datamodel.matchers.BgpRouteMatchersImpl.IsBgpRouteThat;
import org.hamcrest.Matcher;

@ParametersAreNonnullByDefault
public final class BgpRouteMatchers {

  /**
   * Provides a matcher that matches when the supplied {@code subMatcher} matches the {@link
   * org.batfish.datamodel.BgpRoute}'s communities.
   */
  public static @Nonnull Matcher<BgpRoute> hasCommunities(Matcher<? super Set<Long>> subMatcher) {
    return new HasCommunities(subMatcher);
  }

  /**
   * Provides a matcher that matches when the {@link AbstractRoute} is a {@link BgpRoute} matched by
   * the provided {@link subMatcher}.
   */
  public static @Nonnull Matcher<AbstractRoute> isBgpRouteThat(
      Matcher<? super BgpRoute> subMatcher) {
    return new IsBgpRouteThat(subMatcher);
  }

  private BgpRouteMatchers() {}
}

package org.batfish.datamodel.matchers;

import static org.hamcrest.Matchers.equalTo;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.matchers.BgpRouteMatchersImpl.HasCommunities;
import org.batfish.datamodel.matchers.BgpRouteMatchersImpl.HasOriginType;
import org.batfish.datamodel.matchers.BgpRouteMatchersImpl.HasWeight;
import org.batfish.datamodel.matchers.BgpRouteMatchersImpl.IsBgpRouteThat;
import org.hamcrest.Matcher;

@ParametersAreNonnullByDefault
public final class BgpRouteMatchers {

  /**
   * Provides a matcher that matches when the supplied {@code subMatcher} matches the {@link
   * BgpRoute}'s communities.
   */
  public static @Nonnull Matcher<BgpRoute> hasCommunities(
      Matcher<? super Set<Community>> subMatcher) {
    return new HasCommunities(subMatcher);
  }

  /**
   * Provides a matcher that matches when the supplied {@code expectedOriginType} is equal to the
   * {@link BgpRoute}'s originType.
   */
  public static @Nonnull Matcher<BgpRoute> hasOriginType(OriginType expectedOriginType) {
    return new HasOriginType(equalTo(expectedOriginType));
  }

  /**
   * Provides a matcher that matches when the supplied {@code expectedWeight} is equal to the {@link
   * BgpRoute}'s weight.
   */
  public static @Nonnull Matcher<BgpRoute> hasWeight(int expectedWeight) {
    return new HasWeight(equalTo(expectedWeight));
  }

  /**
   * Provides a matcher that matches when the {@link AbstractRoute} is a {@link BgpRoute} matched by
   * the provided {@code subMatcher}.
   */
  public static @Nonnull Matcher<AbstractRoute> isBgpRouteThat(
      Matcher<? super BgpRoute> subMatcher) {
    return new IsBgpRouteThat(subMatcher);
  }

  private BgpRouteMatchers() {}
}

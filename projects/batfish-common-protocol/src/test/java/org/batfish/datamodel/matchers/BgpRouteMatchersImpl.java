package org.batfish.datamodel.matchers;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.bgp.community.Community;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

@ParametersAreNonnullByDefault
final class BgpRouteMatchersImpl {
  static final class HasCommunities extends FeatureMatcher<BgpRoute, Set<Community>> {
    HasCommunities(@Nonnull Matcher<? super Set<Community>> subMatcher) {
      super(subMatcher, "A BgpRoute with communities:", "communities");
    }

    @Override
    protected Set<Community> featureValueOf(BgpRoute actual) {
      return actual.getCommunities();
    }
  }

  static final class HasOriginType extends FeatureMatcher<BgpRoute, OriginType> {
    HasOriginType(@Nonnull Matcher<? super OriginType> subMatcher) {
      super(subMatcher, "A BgpRoute with originType:", "originType");
    }

    @Override
    protected OriginType featureValueOf(BgpRoute actual) {
      return actual.getOriginType();
    }
  }

  static final class HasWeight extends FeatureMatcher<BgpRoute, Integer> {
    HasWeight(@Nonnull Matcher<? super Integer> subMatcher) {
      super(subMatcher, "A BgpRoute with weight:", "weight");
    }

    @Override
    protected Integer featureValueOf(BgpRoute actual) {
      return actual.getWeight();
    }
  }

  static final class IsBgpRouteThat extends IsInstanceThat<AbstractRoute, BgpRoute> {
    IsBgpRouteThat(Matcher<? super BgpRoute> subMatcher) {
      super(BgpRoute.class, subMatcher);
    }
  }

  private BgpRouteMatchersImpl() {}
}

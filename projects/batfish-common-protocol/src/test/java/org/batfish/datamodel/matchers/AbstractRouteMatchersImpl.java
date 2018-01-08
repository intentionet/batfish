package org.batfish.datamodel.matchers;

import javax.annotation.Nonnull;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

final class AbstractRouteMatchersImpl {
  static final class HasMetric extends FeatureMatcher<AbstractRoute, Long> {
    HasMetric(@Nonnull Matcher<? super Long> subMatcher) {
      super(subMatcher, "metric", "metric");
    }

    @Override
    protected Long featureValueOf(AbstractRoute actual) {
      return actual.getMetric();
    }
  }

  static final class HasPrefix extends FeatureMatcher<AbstractRoute, Prefix> {
    HasPrefix(@Nonnull Matcher<? super Prefix> subMatcher) {
      super(subMatcher, "network", "network");
    }

    @Override
    protected Prefix featureValueOf(AbstractRoute actual) {
      return actual.getNetwork();
    }
  }

  static final class HasProtocol extends FeatureMatcher<AbstractRoute, RoutingProtocol> {
    HasProtocol(@Nonnull Matcher<? super RoutingProtocol> subMatcher) {
      super(subMatcher, "protocol", "protocol");
    }

    @Override
    protected RoutingProtocol featureValueOf(AbstractRoute actual) {
      return actual.getProtocol();
    }
  }

  private AbstractRouteMatchersImpl() {}
}

package org.batfish.datamodel;

import static org.hamcrest.Matchers.equalTo;

import javax.annotation.Nonnull;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public final class AbstractRouteMatchers {

  private static final class HasMetric extends FeatureMatcher<AbstractRoute, Long> {

    private HasMetric(@Nonnull Matcher<? super Long> subMatcher) {
      super(subMatcher, "metric", "metric");
    }

    @Override
    protected Long featureValueOf(AbstractRoute actual) {
      return actual.getMetric();
    }
  }

  private static final class HasPrefix extends FeatureMatcher<AbstractRoute, Prefix> {

    private HasPrefix(@Nonnull Matcher<? super Prefix> subMatcher) {
      super(subMatcher, "network", "network");
    }

    @Override
    protected Prefix featureValueOf(AbstractRoute actual) {
      return actual.getNetwork();
    }
  }

  private static final class HasProtocol extends FeatureMatcher<AbstractRoute, RoutingProtocol> {

    private HasProtocol(@Nonnull Matcher<? super RoutingProtocol> subMatcher) {
      super(subMatcher, "protocol", "protocol");
    }

    @Override
    protected RoutingProtocol featureValueOf(AbstractRoute actual) {
      return actual.getProtocol();
    }
  }

  public static Matcher<AbstractRoute> hasMetric(Long expectedMetric) {
    return new HasMetric(equalTo(expectedMetric));
  }

  public static Matcher<AbstractRoute> hasMetric(@Nonnull Matcher<? super Long> subMatcher) {
    return new HasMetric(subMatcher);
  }

  public static Matcher<AbstractRoute> hasPrefix(@Nonnull Matcher<? super Prefix> subMatcher) {
    return new HasPrefix(subMatcher);
  }

  public static Matcher<AbstractRoute> hasPrefix(Prefix expectedPrefix) {
    return new HasPrefix(equalTo(expectedPrefix));
  }

  public static Matcher<AbstractRoute> hasProtocol(
      @Nonnull Matcher<? super RoutingProtocol> subMatcher) {
    return new HasProtocol(subMatcher);
  }

  public static Matcher<AbstractRoute> hasProtocol(RoutingProtocol expectedProtocol) {
    return new HasProtocol(equalTo(expectedProtocol));
  }

  private AbstractRouteMatchers() {}
}

package org.batfish.matchers;

import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.AbstractRouteDecorator;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.dataplane.rib.RouteAdvertisement;
import org.batfish.dataplane.rib.RouteAdvertisement.Reason;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

@ParametersAreNonnullByDefault
public class RouteAdvertisementMatchersImpl {

  static final class HasPrefix
      extends FeatureMatcher<RouteAdvertisement<? extends AbstractRouteDecorator>, Prefix> {
    HasPrefix(Matcher<? super Prefix> subMatcher) {
      super(subMatcher, "A RouteAdvertisement with network:", "network");
    }

    @Override
    protected Prefix featureValueOf(RouteAdvertisement<? extends AbstractRouteDecorator> actual) {
      return actual.getRoute().getNetwork();
    }
  }

  static final class HasNextHopIp
      extends FeatureMatcher<RouteAdvertisement<? extends AbstractRouteDecorator>, Ip> {
    HasNextHopIp(Matcher<? super Ip> subMatcher) {
      super(subMatcher, "A RouteAdvertisement with nextHopIp:", "nextHopIp");
    }

    @Override
    protected Ip featureValueOf(RouteAdvertisement<? extends AbstractRouteDecorator> actual) {
      return actual.getRoute().getAbstractRoute().getNextHopIp();
    }
  }

  static final class HasReason
      extends FeatureMatcher<RouteAdvertisement<? extends AbstractRouteDecorator>, Reason> {
    HasReason(Matcher<? super Reason> subMatcher) {
      super(subMatcher, "A RouteAdvertisement with reason:", "reason");
    }

    @Override
    protected Reason featureValueOf(RouteAdvertisement<? extends AbstractRouteDecorator> actual) {
      return actual.getReason();
    }
  }
}

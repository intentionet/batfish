package org.batfish.datamodel.matchers;

import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.BgpAdvertisement.BgpAdvertisementType;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

final class BgpAdvertisementMatchersImpl {
  static final class HasDestinationIp extends FeatureMatcher<BgpAdvertisement, Ip> {
    HasDestinationIp(Matcher<? super Ip> subMatcher) {
      super(subMatcher, "destinationIp", "destinationIp");
    }

    @Override
    protected Ip featureValueOf(BgpAdvertisement actual) {
      return actual.getDstIp();
    }
  }

  static final class HasNetwork extends FeatureMatcher<BgpAdvertisement, Prefix> {
    HasNetwork(Matcher<? super Prefix> subMatcher) {
      super(subMatcher, "network", "network");
    }

    @Override
    protected Prefix featureValueOf(BgpAdvertisement actual) {
      return actual.getNetwork();
    }
  }

  static final class HasOriginatorIp extends FeatureMatcher<BgpAdvertisement, Ip> {
    HasOriginatorIp(Matcher<? super Ip> subMatcher) {
      super(subMatcher, "originatorIp", "originatorIp");
    }

    @Override
    protected Ip featureValueOf(BgpAdvertisement actual) {
      return actual.getOriginatorIp();
    }
  }

  static final class HasSourceIp extends FeatureMatcher<BgpAdvertisement, Ip> {
    HasSourceIp(Matcher<? super Ip> subMatcher) {
      super(subMatcher, "sourceIp", "sourceIp");
    }

    @Override
    protected Ip featureValueOf(BgpAdvertisement actual) {
      return actual.getSrcIp();
    }
  }

  static final class HasType extends FeatureMatcher<BgpAdvertisement, BgpAdvertisementType> {
    HasType(Matcher<? super BgpAdvertisementType> subMatcher) {
      super(subMatcher, "bgpAdvertisementType", "bgpAdvertisementType");
    }

    @Override
    protected BgpAdvertisementType featureValueOf(BgpAdvertisement actual) {
      return actual.getType();
    }
  }

  private BgpAdvertisementMatchersImpl() {}
}

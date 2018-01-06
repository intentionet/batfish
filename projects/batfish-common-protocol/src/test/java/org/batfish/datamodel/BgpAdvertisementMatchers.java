package org.batfish.datamodel;

import static org.hamcrest.Matchers.equalTo;

import javax.annotation.Nonnull;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.BgpAdvertisement.BgpAdvertisementType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public final class BgpAdvertisementMatchers {
  private static final class HasDestinationIp extends FeatureMatcher<BgpAdvertisement, Ip> {

    private HasDestinationIp(Matcher<? super Ip> subMatcher) {
      super(subMatcher, "destinationIp", "destinationIp");
    }

    @Override
    protected Ip featureValueOf(BgpAdvertisement actual) {
      return actual.getDstIp();
    }
  }

  private static final class HasNetwork extends FeatureMatcher<BgpAdvertisement, Prefix> {

    private HasNetwork(Matcher<? super Prefix> subMatcher) {
      super(subMatcher, "network", "network");
    }

    @Override
    protected Prefix featureValueOf(BgpAdvertisement actual) {
      return actual.getNetwork();
    }
  }

  private static final class HasOriginatorIp extends FeatureMatcher<BgpAdvertisement, Ip> {

    private HasOriginatorIp(Matcher<? super Ip> subMatcher) {
      super(subMatcher, "originatorIp", "originatorIp");
    }

    @Override
    protected Ip featureValueOf(BgpAdvertisement actual) {
      return actual.getOriginatorIp();
    }
  }

  private static final class HasSourceIp extends FeatureMatcher<BgpAdvertisement, Ip> {

    private HasSourceIp(Matcher<? super Ip> subMatcher) {
      super(subMatcher, "sourceIp", "sourceIp");
    }

    @Override
    protected Ip featureValueOf(BgpAdvertisement actual) {
      return actual.getSrcIp();
    }
  }

  private static final class HasType
      extends FeatureMatcher<BgpAdvertisement, BgpAdvertisementType> {

    private HasType(Matcher<? super BgpAdvertisementType> subMatcher) {
      super(subMatcher, "bgpAdvertisementType", "bgpAdvertisementType");
    }

    @Override
    protected BgpAdvertisementType featureValueOf(BgpAdvertisement actual) {
      return actual.getType();
    }
  }

  public static Matcher<BgpAdvertisement> hasDestinationIp(Ip expectedDestinationIp) {
    return new HasDestinationIp(equalTo(expectedDestinationIp));
  }

  public static Matcher<BgpAdvertisement> hasDestinationIp(
      @Nonnull Matcher<? super Ip> subMatcher) {
    return new HasDestinationIp(subMatcher);
  }

  public static Matcher<BgpAdvertisement> hasNetwork(@Nonnull Matcher<? super Prefix> subMatcher) {
    return new HasNetwork(subMatcher);
  }

  public static Matcher<BgpAdvertisement> hasNetwork(Prefix expectedNetwork) {
    return new HasNetwork(equalTo(expectedNetwork));
  }

  public static Matcher<BgpAdvertisement> hasOriginatorIp(Ip expectedOriginatorIp) {
    return new HasOriginatorIp(equalTo(expectedOriginatorIp));
  }

  public static Matcher<BgpAdvertisement> hasOriginatorIp(@Nonnull Matcher<? super Ip> subMatcher) {
    return new HasOriginatorIp(subMatcher);
  }

  public static Matcher<BgpAdvertisement> hasSourceIp(Ip expectedSourceIp) {
    return new HasSourceIp(equalTo(expectedSourceIp));
  }

  public static Matcher<BgpAdvertisement> hasSourceIp(@Nonnull Matcher<? super Ip> subMatcher) {
    return new HasSourceIp(subMatcher);
  }

  public static Matcher<BgpAdvertisement> hasType(BgpAdvertisementType expectedType) {
    return new HasType(equalTo(expectedType));
  }

  public static Matcher<BgpAdvertisement> hasType(
      @Nonnull Matcher<? super BgpAdvertisementType> subMatcher) {
    return new HasType(subMatcher);
  }

  private BgpAdvertisementMatchers() {}
}

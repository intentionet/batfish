package org.batfish.datamodel.matchers;

import static org.hamcrest.Matchers.equalTo;

import javax.annotation.Nonnull;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.BgpAdvertisement.BgpAdvertisementType;
import org.batfish.datamodel.matchers.BgpAdvertisementMatchersImpl.HasDestinationIp;
import org.batfish.datamodel.matchers.BgpAdvertisementMatchersImpl.HasNetwork;
import org.batfish.datamodel.matchers.BgpAdvertisementMatchersImpl.HasOriginatorIp;
import org.batfish.datamodel.matchers.BgpAdvertisementMatchersImpl.HasSourceIp;
import org.batfish.datamodel.matchers.BgpAdvertisementMatchersImpl.HasType;
import org.hamcrest.Matcher;

public final class BgpAdvertisementMatchers {

  /**
   * Provides a matcher that matches when {@code expectedDestinationIp} is equal to the {@link
   * BgpAdvertisement}'s destination IP.
   */
  public static HasDestinationIp hasDestinationIp(Ip expectedDestinationIp) {
    return new HasDestinationIp(equalTo(expectedDestinationIp));
  }

  /**
   * Provides a matcher that matches when the supplied {@code subMatcher} matches the {@link
   * BgpAdvertisement}'s destination IP.
   */
  public static HasDestinationIp hasDestinationIp(@Nonnull Matcher<? super Ip> subMatcher) {
    return new HasDestinationIp(subMatcher);
  }

  /**
   * Provides a matcher that matches when the supplied {@code subMatcher} matches the {@link
   * BgpAdvertisement}'s network.
   */
  public static HasNetwork hasNetwork(@Nonnull Matcher<? super Prefix> subMatcher) {
    return new HasNetwork(subMatcher);
  }

  /**
   * Provides a matcher that matches when {@code expectedNetwork} is equal to the {@link
   * BgpAdvertisement}'s network.
   */
  public static HasNetwork hasNetwork(Prefix expectedNetwork) {
    return new HasNetwork(equalTo(expectedNetwork));
  }

  /**
   * Provides a matcher that matches when {@code expectedOriginatorIp} is equal to the {@link
   * BgpAdvertisement}'s originator IP.
   */
  public static HasOriginatorIp hasOriginatorIp(Ip expectedOriginatorIp) {
    return new HasOriginatorIp(equalTo(expectedOriginatorIp));
  }

  /**
   * Provides a matcher that matches when the supplied {@code subMatcher} matches the {@link
   * BgpAdvertisement}'s originator IP.
   */
  public static HasOriginatorIp hasOriginatorIp(@Nonnull Matcher<? super Ip> subMatcher) {
    return new HasOriginatorIp(subMatcher);
  }

  /**
   * Provides a matcher that matches when {@code expectedSourceIp} is equal to the {@link
   * BgpAdvertisement}'s source IP.
   */
  public static HasSourceIp hasSourceIp(Ip expectedSourceIp) {
    return new HasSourceIp(equalTo(expectedSourceIp));
  }

  /**
   * Provides a matcher that matches when the supplied {@code subMatcher} matches the {@link
   * BgpAdvertisement}'s source IP.
   */
  public static HasSourceIp hasSourceIp(@Nonnull Matcher<? super Ip> subMatcher) {
    return new HasSourceIp(subMatcher);
  }

  /**
   * Provides a matcher that matches when {@code expectedType} is equal to the {@link
   * BgpAdvertisement}'s type.
   */
  public static HasType hasType(BgpAdvertisementType expectedType) {
    return new HasType(equalTo(expectedType));
  }

  /**
   * Provides a matcher that matches when the supplied {@code subMatcher} matches the {@link
   * BgpAdvertisement}'s type.
   */
  public static HasType hasType(@Nonnull Matcher<? super BgpAdvertisementType> subMatcher) {
    return new HasType(subMatcher);
  }

  private BgpAdvertisementMatchers() {}
}

package org.batfish.datamodel.matchers;

import static org.hamcrest.Matchers.equalTo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.IsoAddress;
import org.batfish.datamodel.isis.IsisInterfaceLevelSettings;
import org.batfish.datamodel.isis.IsisInterfaceSettings;
import org.batfish.datamodel.matchers.IsisInterfaceSettingsMatchersImpl.HasBfdLivenessDetectionMinimumInterval;
import org.batfish.datamodel.matchers.IsisInterfaceSettingsMatchersImpl.HasBfdLivenessDetectionMultiplier;
import org.batfish.datamodel.matchers.IsisInterfaceSettingsMatchersImpl.HasIsoAddress;
import org.batfish.datamodel.matchers.IsisInterfaceSettingsMatchersImpl.HasLevel1;
import org.batfish.datamodel.matchers.IsisInterfaceSettingsMatchersImpl.HasLevel2;
import org.batfish.datamodel.matchers.IsisInterfaceSettingsMatchersImpl.HasPointToPoint;
import org.hamcrest.Matcher;

public final class IsisInterfaceSettingsMatchers {

  /**
   * Provides a matcher that matches if the {@link IsisInterfaceSettings}'s
   * bfdLivenessDetectionMinimumInterval is {@code expectedBfdLivenessDetectionMinimumInterval}.
   */
  public static @Nonnull Matcher<IsisInterfaceSettings> hasBfdLivenessDetectionMinimumInterval(
      @Nullable Integer expectedBfdLivenessDetectionMinimumInterval) {
    return new HasBfdLivenessDetectionMinimumInterval(
        equalTo(expectedBfdLivenessDetectionMinimumInterval));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link
   * IsisInterfaceSettings}'s bfdLivenessDetectionMinimumInterval.
   */
  public static @Nonnull Matcher<IsisInterfaceSettings> hasBfdLivenessDetectionMinimumInterval(
      @Nonnull Matcher<? super Integer> subMatcher) {
    return new HasBfdLivenessDetectionMinimumInterval(subMatcher);
  }

  /**
   * Provides a matcher that matches if the {@link IsisInterfaceSettings}'s
   * bfdLivenessDetectionMultiplier is {@code expectedBfdLivenessDetectionMultiplier}.
   */
  public static @Nonnull Matcher<IsisInterfaceSettings> hasBfdLivenessDetectionMultiplier(
      @Nullable Integer expectedBfdLivenessDetectionMultiplier) {
    return new HasBfdLivenessDetectionMultiplier(equalTo(expectedBfdLivenessDetectionMultiplier));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link
   * IsisInterfaceSettings}'s bfdLivenessDetectionMultiplier.
   */
  public static @Nonnull Matcher<IsisInterfaceSettings> hasBfdLivenessDetectionMultiplier(
      @Nonnull Matcher<? super Integer> subMatcher) {
    return new HasBfdLivenessDetectionMultiplier(subMatcher);
  }

  /**
   * Provides a matcher that matches if the {@link IsisInterfaceSettings}'s isoAddress is {@code
   * expectedIsoAddress}.
   */
  public static @Nonnull Matcher<IsisInterfaceSettings> hasIsoAddress(
      @Nullable IsoAddress expectedIsoAddress) {
    return new HasIsoAddress(equalTo(expectedIsoAddress));
  }

  /**
   * Provides a matcher that matches if the provied {@code subMatcher} matches the {@link
   * IsisInterfaceSettings}'s isoAddress.
   */
  public static @Nonnull Matcher<IsisInterfaceSettings> hasIsoAddress(
      @Nonnull Matcher<? super IsoAddress> subMatcher) {
    return new HasIsoAddress(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link
   * IsisInterfaceSettings}'s level1.
   */
  public static @Nonnull Matcher<IsisInterfaceSettings> hasLevel1(
      @Nonnull Matcher<? super IsisInterfaceLevelSettings> subMatcher) {
    return new HasLevel1(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link
   * IsisInterfaceSettings}'s level2.
   */
  public static @Nonnull Matcher<IsisInterfaceSettings> hasLevel2(
      @Nonnull Matcher<? super IsisInterfaceLevelSettings> subMatcher) {
    return new HasLevel2(subMatcher);
  }

  /** Provides a matcher that matches if the {@link IsisInterfaceSettings}'s is pointToPoint. */
  public static @Nonnull Matcher<IsisInterfaceSettings> hasPointToPoint() {
    return new HasPointToPoint(equalTo(true));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link
   * IsisInterfaceSettings}'s pointToPoint.
   */
  public static @Nonnull Matcher<IsisInterfaceSettings> hasPointToPoint(
      @Nonnull Matcher<? super Boolean> subMatcher) {
    return new HasPointToPoint(subMatcher);
  }

  private IsisInterfaceSettingsMatchers() {}
}

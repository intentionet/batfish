package org.batfish.datamodel.matchers;

import static org.hamcrest.Matchers.equalTo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.isis.IsisHelloAuthenticationType;
import org.batfish.datamodel.isis.IsisInterfaceLevelSettings;
import org.batfish.datamodel.isis.IsisInterfaceMode;
import org.batfish.datamodel.isis.IsisInterfaceSettings;
import org.batfish.datamodel.matchers.IsisInterfaceLevelSettingsMatchersImpl.HasCost;
import org.batfish.datamodel.matchers.IsisInterfaceLevelSettingsMatchersImpl.HasHelloAuthenticationType;
import org.batfish.datamodel.matchers.IsisInterfaceLevelSettingsMatchersImpl.HasHelloInterval;
import org.batfish.datamodel.matchers.IsisInterfaceLevelSettingsMatchersImpl.HasHoldTime;
import org.batfish.datamodel.matchers.IsisInterfaceLevelSettingsMatchersImpl.HasMode;
import org.hamcrest.Matcher;

public final class IsisInterfaceLevelSettingsMatchers {

  /**
   * Provides a matcher that matches when the {@link IsisInterfaceLevelSettings}'s cost is {@code
   * expectedCost}.
   */
  public static @Nonnull Matcher<IsisInterfaceLevelSettings> hasCost(@Nullable Long expectedCost) {
    return new HasCost(equalTo(expectedCost));
  }

  /**
   * Provides a matcher that matches when the provided {@code subMatcher} matches the {@link
   * IsisInterfaceLevelSettings}'s cost.
   */
  public static @Nonnull Matcher<IsisInterfaceLevelSettings> hasCost(
      @Nonnull Matcher<? super Long> subMatcher) {
    return new HasCost(subMatcher);
  }

  /**
   * Provides a matcher that matches when the {@link IsisInterfaceLevelSettings}'s
   * helloAuthenticationType is {@code expectedHelloAuthenticationType}.
   */
  public static @Nonnull Matcher<IsisInterfaceLevelSettings> hasHelloAuthenticationType(
      @Nullable IsisHelloAuthenticationType expectedHelloAuthenticationType) {
    return new HasHelloAuthenticationType(equalTo(expectedHelloAuthenticationType));
  }

  /**
   * Provides a matcher that matches when the provided {@code subMatcher} matches the {@link
   * IsisInterfaceLevelSettings}'s helloAuthenticationType.
   */
  public static @Nonnull Matcher<IsisInterfaceLevelSettings> hasHelloAuthenticationType(
      @Nonnull Matcher<? super IsisHelloAuthenticationType> subMatcher) {
    return new HasHelloAuthenticationType(subMatcher);
  }

  /**
   * Provides a matcher that matches if the {@link IsisInterfaceSettings}'s helloInterval is {@code
   * expectedHelloInterval}.
   */
  public static @Nonnull Matcher<IsisInterfaceLevelSettings> hasHelloInterval(
      @Nullable Integer expectedHelloInterval) {
    return new HasHelloInterval(equalTo(expectedHelloInterval));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link
   * IsisInterfaceSettings}'s helloInterval.
   */
  public static @Nonnull Matcher<IsisInterfaceLevelSettings> hasHelloInterval(
      @Nonnull Matcher<? super Integer> subMatcher) {
    return new HasHelloInterval(subMatcher);
  }

  /**
   * Provides a matcher that matches when the {@link IsisInterfaceLevelSettings}'s holdTime is
   * {@code expectedHoldTime}.
   */
  public static @Nonnull Matcher<IsisInterfaceLevelSettings> hasHoldTime(
      @Nullable Integer expectedHoldTime) {
    return new HasHoldTime(equalTo(expectedHoldTime));
  }

  /**
   * Provides a matcher that matches when the provided {@code subMatcher} matches the {@link
   * IsisInterfaceLevelSettings}'s holdTime.
   */
  public static @Nonnull Matcher<IsisInterfaceLevelSettings> hasHoldTime(
      @Nonnull Matcher<? super Integer> subMatcher) {
    return new HasHoldTime(subMatcher);
  }

  /**
   * Provides a matcher that matches when the {@link IsisInterfaceLevelSettings}'s mode is {@code
   * expectedMode}.
   */
  public static @Nonnull Matcher<IsisInterfaceLevelSettings> hasMode(
      @Nullable IsisInterfaceMode expectedMode) {
    return new HasMode(equalTo(expectedMode));
  }

  /**
   * Provides a matcher that matches when the provided {@code subMatcher} matches the {@link
   * IsisInterfaceLevelSettings}'s mode.
   */
  public static @Nonnull Matcher<IsisInterfaceLevelSettings> hasMode(
      @Nonnull Matcher<? super IsisInterfaceMode> subMatcher) {
    return new HasMode(subMatcher);
  }

  private IsisInterfaceLevelSettingsMatchers() {}
}

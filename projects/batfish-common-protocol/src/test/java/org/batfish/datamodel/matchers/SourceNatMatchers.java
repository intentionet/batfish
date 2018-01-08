package org.batfish.datamodel.matchers;

import static org.hamcrest.Matchers.equalTo;

import javax.annotation.Nonnull;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.matchers.SourceNatMatchersImpl.HasPoolIpFirst;
import org.batfish.datamodel.matchers.SourceNatMatchersImpl.HasPoolIpLast;
import org.hamcrest.Matcher;

public final class SourceNatMatchers {

  /**
   * Provides a matcher that matches when {@code expectedPoolIpFirst} is equal to the {@link
   * SourceNat}'s first pool-ip.
   */
  public static HasPoolIpFirst hasPoolIpFirst(Ip expectedPoolIpFirst) {
    return new HasPoolIpFirst(equalTo(expectedPoolIpFirst));
  }

  /**
   * Provides a matcher that matches when the supplied {@code subMatcher} matches the {@link
   * SourceNat}'s first pool-ip.
   */
  public static HasPoolIpFirst hasPoolIpFirst(@Nonnull Matcher<? super Ip> subMatcher) {
    return new HasPoolIpFirst(subMatcher);
  }

  /**
   * Provides a matcher that matches when {@code expectedPoolIpLast} is equal to the {@link
   * SourceNat}'s last pool-ip.
   */
  public static HasPoolIpLast hasPoolIpLast(Ip expectedPoolIpLast) {
    return new HasPoolIpLast(equalTo(expectedPoolIpLast));
  }

  /**
   * Provides a matcher that matches when the supplied {@code subMatcher} matches the {@link
   * SourceNat}'s last pool-ip.
   */
  public static HasPoolIpLast hasPoolIpLast(@Nonnull Matcher<? super Ip> subMatcher) {
    return new HasPoolIpLast(subMatcher);
  }

  private SourceNatMatchers() {}
}

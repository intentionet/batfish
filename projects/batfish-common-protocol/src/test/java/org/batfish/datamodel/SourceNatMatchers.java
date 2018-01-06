package org.batfish.datamodel;

import javax.annotation.Nonnull;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public final class SourceNatMatchers {
  private static final class HasPoolIpFirst extends FeatureMatcher<SourceNat, Ip> {
    private HasPoolIpFirst(@Nonnull Matcher<? super Ip> subMatcher) {
      super(subMatcher, "poolIpFirst", "poolIpFirst");
    }

    @Override
    protected Ip featureValueOf(SourceNat actual) {
      return actual.getPoolIpFirst();
    }
  }

  public static final class HasPoolIpLast extends FeatureMatcher<SourceNat, Ip> {
    private HasPoolIpLast(@Nonnull Matcher<? super Ip> subMatcher) {
      super(subMatcher, "poolIpLast", "poolIpLast");
    }

    @Override
    protected Ip featureValueOf(SourceNat actual) {
      return actual.getPoolIpLast();
    }
  }

  public static HasPoolIpFirst hasPoolIpFirst(@Nonnull Matcher<? super Ip> subMatcher) {
    return new HasPoolIpFirst(subMatcher);
  }

  public static HasPoolIpLast hasPoolIpLast(@Nonnull Matcher<? super Ip> subMatcher) {
    return new HasPoolIpLast(subMatcher);
  }

  private SourceNatMatchers() {}
}

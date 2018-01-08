package org.batfish.datamodel.matchers;

import javax.annotation.Nonnull;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.SourceNat;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

final class SourceNatMatchersImpl {
  static final class HasPoolIpFirst extends FeatureMatcher<SourceNat, Ip> {
    HasPoolIpFirst(@Nonnull Matcher<? super Ip> subMatcher) {
      super(subMatcher, "poolIpFirst", "poolIpFirst");
    }

    @Override
    protected Ip featureValueOf(SourceNat actual) {
      return actual.getPoolIpFirst();
    }
  }

  static final class HasPoolIpLast extends FeatureMatcher<SourceNat, Ip> {
    HasPoolIpLast(@Nonnull Matcher<? super Ip> subMatcher) {
      super(subMatcher, "poolIpLast", "poolIpLast");
    }

    @Override
    protected Ip featureValueOf(SourceNat actual) {
      return actual.getPoolIpLast();
    }
  }

  private SourceNatMatchersImpl() {}
}

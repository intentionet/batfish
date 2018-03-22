package org.batfish.datamodel.matchers;

import javax.annotation.Nonnull;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpLink;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public class LinkMatchers {

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches {@link
   * IpLink#getIp1}.
   */
  public static HasIp1 hasIp1(Matcher<? super Ip> subMatcher) {
    return new HasIp1(subMatcher);
  }

  private static final class HasIp1 extends FeatureMatcher<IpLink, Ip> {
    HasIp1(@Nonnull Matcher<? super Ip> subMatcher) {
      super(subMatcher, "An IpLink with ip1:", "ip1");
    }

    @Override
    protected Ip featureValueOf(IpLink actual) {
      return actual.getIp1();
    }
  }

  private LinkMatchers() {}
}

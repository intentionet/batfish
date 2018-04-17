package org.batfish.datamodel.matchers;

import javax.annotation.Nonnull;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.matchers.HeaderSpaceMatchersImpl.HasDstIps;
import org.batfish.datamodel.matchers.HeaderSpaceMatchersImpl.HasSrcIps;
import org.hamcrest.Matcher;

public class HeaderSpaceMatchers {

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the HeaderSpace's
   * dstIps.
   */
  public static HasDstIps hasDstIps(@Nonnull Matcher<? super IpSpace> subMatcher) {
    return new HasDstIps(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the HeaderSpace's
   * srcIps.
   */
  public static HasSrcIps hasSrcIps(@Nonnull Matcher<? super IpSpace> subMatcher) {
    return new HasSrcIps(subMatcher);
  }

  private HeaderSpaceMatchers() {}
}

package org.batfish.datamodel.vendor_family.cisco;

import org.batfish.datamodel.vendor_family.cisco.CiscoFamilyMatchersImpl.HasLogging;
import org.hamcrest.Matcher;

public class CiscoFamilyMatchers {

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the CiscoFamily's
   * logging.
   */
  public static HasLogging hasLogging(Matcher<? super Logging> subMatcher) {
    return new HasLogging(subMatcher);
  }
}

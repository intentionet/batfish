package org.batfish.datamodel.vendor_family;

import org.batfish.datamodel.vendor_family.VendorFamilyMatchersImpl.HasCisco;
import org.batfish.datamodel.vendor_family.cisco.CiscoFamily;
import org.hamcrest.Matcher;

public class VendorFamilyMatchers {

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the VendorFamily's
   * cisco
   */
  public static HasCisco hasCisco(Matcher<? super CiscoFamily> subMatcher) {
    return new HasCisco(subMatcher);
  }

  private VendorFamilyMatchers() {}
}

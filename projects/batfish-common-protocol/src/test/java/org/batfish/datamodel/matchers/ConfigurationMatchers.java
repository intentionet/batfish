package org.batfish.datamodel.matchers;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.Map;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasDefaultVrf;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasVendorFamily;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasVrfs;
import org.batfish.datamodel.vendor_family.VendorFamily;
import org.hamcrest.Matcher;

public class ConfigurationMatchers {

  /** Provides a matcher that matches if the configuration has a default VRF. */
  public static HasDefaultVrf hasDefaultVrf() {
    return new HasDefaultVrf(not(nullValue()));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * default VRF.
   */
  public static HasDefaultVrf hasDefaultVrf(Matcher<? super Vrf> subMatcher) {
    return new HasDefaultVrf(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * vendorFamily.
   */
  public static HasVendorFamily hasVendorFamily(Matcher<? super VendorFamily> subMatcher) {
    return new HasVendorFamily(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * vrfs.
   */
  public static HasVrfs hasVrfs(Matcher<? super Map<String, Vrf>> subMatcher) {
    return new HasVrfs(subMatcher);
  }

  private ConfigurationMatchers() {}
}

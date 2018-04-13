package org.batfish.datamodel.matchers;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.Map;
import javax.annotation.Nonnull;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasDefaultVrf;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasInterface;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasInterfaces;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasIpAccessLists;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasVendorFamily;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasVrf;
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
   * interface with specified name.
   */
  public static HasInterface hasInterface(
      @Nonnull String name, @Nonnull Matcher<? super Interface> subMatcher) {
    return new HasInterface(name, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * interfaces.
   */
  public static HasInterfaces hasInterfaces(
      @Nonnull Matcher<? super Map<String, Interface>> subMatcher) {
    return new HasInterfaces(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * ipAccessLists.
   */
  public static HasIpAccessLists hasIpAccessLists(
      Matcher<? super Map<String, IpAccessList>> subMatcher) {
    return new HasIpAccessLists(subMatcher);
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
   * VRF with specified name.
   */
  public static HasVrf hasVrf(String name, Matcher<? super Vrf> subMatcher) {
    return new HasVrf(name, subMatcher);
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

package org.batfish.datamodel.matchers;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.Map;
import javax.annotation.Nonnull;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasDefaultVrf;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasInterface;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasInterfaces;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasIpAccessList;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasIpAccessLists;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasIpSpace;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasIpSpaces;
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
  public static HasDefaultVrf hasDefaultVrf(@Nonnull Matcher<? super Vrf> subMatcher) {
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
   * IpAccessList with specified name.
   */
  public static HasIpAccessList hasIpAccessList(
      @Nonnull String name, @Nonnull Matcher<? super IpAccessList> subMatcher) {
    return new HasIpAccessList(name, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * ipAccessLists.
   */
  public static HasIpAccessLists hasIpAccessLists(
      @Nonnull Matcher<? super Map<String, IpAccessList>> subMatcher) {
    return new HasIpAccessLists(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * IpSpace with specified name.
   */
  public static HasIpSpace hasIpSpace(
      @Nonnull String name, @Nonnull Matcher<? super IpSpace> subMatcher) {
    return new HasIpSpace(name, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * ipSpaces.
   */
  public static HasIpSpaces hasIpSpaces(@Nonnull Matcher<? super Map<String, IpSpace>> subMatcher) {
    return new HasIpSpaces(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * vendorFamily.
   */
  public static HasVendorFamily hasVendorFamily(@Nonnull Matcher<? super VendorFamily> subMatcher) {
    return new HasVendorFamily(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * VRF with specified name.
   */
  public static HasVrf hasVrf(@Nonnull String name, @Nonnull Matcher<? super Vrf> subMatcher) {
    return new HasVrf(name, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * vrfs.
   */
  public static HasVrfs hasVrfs(@Nonnull Matcher<? super Map<String, Vrf>> subMatcher) {
    return new HasVrfs(subMatcher);
  }

  private ConfigurationMatchers() {}
}

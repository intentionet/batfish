package org.batfish.datamodel.matchers;

import java.util.Map;
import javax.annotation.Nonnull;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.vendor_family.VendorFamily;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

final class ConfigurationMatchersImpl {

  static final class HasDefaultVrf extends FeatureMatcher<Configuration, Vrf> {
    HasDefaultVrf(@Nonnull Matcher<? super Vrf> subMatcher) {
      super(subMatcher, "default vrf", "default vrf");
    }

    @Override
    protected Vrf featureValueOf(Configuration actual) {
      return actual.getDefaultVrf();
    }
  }

  static final class HasVendorFamily extends FeatureMatcher<Configuration, VendorFamily> {
    HasVendorFamily(@Nonnull Matcher<? super VendorFamily> subMatcher) {
      super(subMatcher, "a configuration with vendorFamily", "vendorFamily");
    }

    @Override
    protected VendorFamily featureValueOf(Configuration actual) {
      return actual.getVendorFamily();
    }
  }

  static final class HasVrfs extends FeatureMatcher<Configuration, Map<String, Vrf>> {
    HasVrfs(@Nonnull Matcher<? super Map<String, Vrf>> subMatcher) {
      super(subMatcher, "a configuration with vrfs", "vrfs");
    }

    @Override
    protected Map<String, Vrf> featureValueOf(Configuration actual) {
      return actual.getVrfs();
    }
  }

  private ConfigurationMatchersImpl() {}
}

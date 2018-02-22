package org.batfish.datamodel.vendor_family;

import javax.annotation.Nonnull;
import org.batfish.datamodel.vendor_family.cisco.CiscoFamily;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public class VendorFamilyMatchersImpl {

  static final class HasCisco extends FeatureMatcher<VendorFamily, CiscoFamily> {
    HasCisco(@Nonnull Matcher<? super CiscoFamily> subMatcher) {
      super(subMatcher, "VendorFamily with cisco", "cisco");
    }

    @Override
    protected CiscoFamily featureValueOf(VendorFamily actual) {
      return actual.getCisco();
    }
  }

  private VendorFamilyMatchersImpl() {}
}

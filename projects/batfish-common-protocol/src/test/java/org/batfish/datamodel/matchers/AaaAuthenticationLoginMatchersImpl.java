package org.batfish.datamodel.matchers;

import java.util.SortedMap;
import javax.annotation.Nonnull;
import org.batfish.datamodel.vendor_family.cisco.AaaAuthenticationLogin;
import org.batfish.datamodel.vendor_family.cisco.AaaAuthenticationLoginList;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public class AaaAuthenticationLoginMatchersImpl {

  static final class HasLists
      extends FeatureMatcher<
          AaaAuthenticationLogin, SortedMap<String, AaaAuthenticationLoginList>> {
    HasLists(@Nonnull Matcher<? super SortedMap<String, AaaAuthenticationLoginList>> subMatcher) {
      super(subMatcher, "AaaAuthenticationLogin with method lists", "AaaAuthenticationLogin lists");
    }

    @Override
    protected SortedMap<String, AaaAuthenticationLoginList> featureValueOf(
        AaaAuthenticationLogin actual) {
      return actual.getLists();
    }
  }

  static final class HasListForKey
      extends FeatureMatcher<AaaAuthenticationLogin, AaaAuthenticationLoginList> {
    private String _key;

    HasListForKey(@Nonnull Matcher<? super AaaAuthenticationLoginList> subMatcher, String key) {
      super(
          subMatcher,
          "AaaAuthenticationLogin has list for key",
          "AaaAuthenticationLogin list for key");
      _key = key;
    }

    @Override
    protected AaaAuthenticationLoginList featureValueOf(AaaAuthenticationLogin actual) {
      return actual.getLists().get(_key);
    }
  }
}

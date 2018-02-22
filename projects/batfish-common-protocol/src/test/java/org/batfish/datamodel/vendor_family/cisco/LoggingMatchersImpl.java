package org.batfish.datamodel.vendor_family.cisco;

import javax.annotation.Nonnull;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public class LoggingMatchersImpl {

  static final class HasOn extends FeatureMatcher<Logging, Boolean> {
    HasOn(@Nonnull Matcher<? super Boolean> subMatcher) {
      super(subMatcher, "is logging with on", "on");
    }

    @Override
    protected Boolean featureValueOf(Logging actual) {
      return actual.getOn();
    }
  }

  private LoggingMatchersImpl() {}
}

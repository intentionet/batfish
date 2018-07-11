package org.batfish.datamodel.matchers;

import org.batfish.datamodel.isis.IsisLevelSettings;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

final class IsisLevelSettingsMatchersImpl {
  static final class HasWideMetricsOnly extends FeatureMatcher<IsisLevelSettings, Boolean> {
    HasWideMetricsOnly(Matcher<? super Boolean> subMatcher) {
      super(subMatcher, "An IsisLevelSettings with wideMetricsOnly:", "wideMetricsOnly");
    }

    @Override
    protected Boolean featureValueOf(IsisLevelSettings actual) {
      return actual.getWideMetricsOnly();
    }
  }

  private IsisLevelSettingsMatchersImpl() {}
}

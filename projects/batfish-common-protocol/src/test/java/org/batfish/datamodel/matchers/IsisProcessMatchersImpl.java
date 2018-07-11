package org.batfish.datamodel.matchers;

import javax.annotation.Nonnull;
import org.batfish.datamodel.isis.IsisLevelSettings;
import org.batfish.datamodel.isis.IsisProcess;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

final class IsisProcessMatchersImpl {

  static final class HasLevel1 extends FeatureMatcher<IsisProcess, IsisLevelSettings> {
    HasLevel1(@Nonnull Matcher<? super IsisLevelSettings> subMatcher) {
      super(subMatcher, "An IsisProcess with level1:", "level1");
    }

    @Override
    protected IsisLevelSettings featureValueOf(IsisProcess actual) {
      return actual.getLevel1();
    }
  }

  static final class HasLevel2 extends FeatureMatcher<IsisProcess, IsisLevelSettings> {
    HasLevel2(@Nonnull Matcher<? super IsisLevelSettings> subMatcher) {
      super(subMatcher, "An IsisProcess with level2:", "level2");
    }

    @Override
    protected IsisLevelSettings featureValueOf(IsisProcess actual) {
      return actual.getLevel2();
    }
  }

  static final class HasReferenceBandwidth extends FeatureMatcher<IsisProcess, Double> {
    HasReferenceBandwidth(@Nonnull Matcher<? super Double> subMatcher) {
      super(subMatcher, "An IsisProcess with referenceBandwidth:", "referenceBandwidth");
    }

    @Override
    protected Double featureValueOf(IsisProcess actual) {
      return actual.getReferenceBandwidth();
    }
  }

  static final class HasOverloadTimeout extends FeatureMatcher<IsisProcess, Integer> {
    HasOverloadTimeout(@Nonnull Matcher<? super Integer> subMatcher) {
      super(subMatcher, "An IsisProcess with overloadTimeout:", "overloadTimeout");
    }

    @Override
    protected Integer featureValueOf(IsisProcess actual) {
      return actual.getOverloadTimeout();
    }
  }

  private IsisProcessMatchersImpl() {}
}

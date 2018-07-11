package org.batfish.datamodel.matchers;

import javax.annotation.Nonnull;
import org.batfish.datamodel.IsoAddress;
import org.batfish.datamodel.isis.IsisInterfaceLevelSettings;
import org.batfish.datamodel.isis.IsisInterfaceSettings;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

final class IsisInterfaceSettingsMatchersImpl {
  static final class HasBfdLivenessDetectionMinimumInterval
      extends FeatureMatcher<IsisInterfaceSettings, Integer> {
    public HasBfdLivenessDetectionMinimumInterval(@Nonnull Matcher<? super Integer> subMatcher) {
      super(
          subMatcher,
          "An IsisInterfaceSettings with bfdLivenessDetectionMinimumInterval:",
          "bfdLivenessDetectionMinimumInterval");
    }

    @Override
    protected Integer featureValueOf(IsisInterfaceSettings actual) {
      return actual.getBfdLivenessDetectionMinimumInterval();
    }
  }

  static final class HasBfdLivenessDetectionMultiplier
      extends FeatureMatcher<IsisInterfaceSettings, Integer> {
    public HasBfdLivenessDetectionMultiplier(@Nonnull Matcher<? super Integer> subMatcher) {
      super(
          subMatcher,
          "An IsisInterfaceSettings with bfdLivenessDetectionMultiplier:",
          "bfdLivenessDetectionMultiplier");
    }

    @Override
    protected Integer featureValueOf(IsisInterfaceSettings actual) {
      return actual.getBfdLivenessDetectionMultiplier();
    }
  }

  static final class HasPointToPoint extends FeatureMatcher<IsisInterfaceSettings, Boolean> {
    public HasPointToPoint(@Nonnull Matcher<? super Boolean> subMatcher) {
      super(subMatcher, "An IsisInterfaceSettings with pointToPoint:", "pointToPoint");
    }

    @Override
    protected Boolean featureValueOf(IsisInterfaceSettings actual) {
      return actual.getPointToPoint();
    }
  }

  static final class HasIsoAddress extends FeatureMatcher<IsisInterfaceSettings, IsoAddress> {
    public HasIsoAddress(@Nonnull Matcher<? super IsoAddress> subMatcher) {
      super(subMatcher, "An IsisInterfaceSettings with isoAddress:", "isoAddress");
    }

    @Override
    protected IsoAddress featureValueOf(IsisInterfaceSettings actual) {
      return actual.getIsoAddress();
    }
  }

  static final class HasLevel1
      extends FeatureMatcher<IsisInterfaceSettings, IsisInterfaceLevelSettings> {
    public HasLevel1(@Nonnull Matcher<? super IsisInterfaceLevelSettings> subMatcher) {
      super(subMatcher, "An IsisInterfaceSettings with level1:", "level1");
    }

    @Override
    protected IsisInterfaceLevelSettings featureValueOf(IsisInterfaceSettings actual) {
      return actual.getLevel1();
    }
  }

  static final class HasLevel2
      extends FeatureMatcher<IsisInterfaceSettings, IsisInterfaceLevelSettings> {
    public HasLevel2(@Nonnull Matcher<? super IsisInterfaceLevelSettings> subMatcher) {
      super(subMatcher, "An IsisInterfaceSettings with level2:", "level2");
    }

    @Override
    protected IsisInterfaceLevelSettings featureValueOf(IsisInterfaceSettings actual) {
      return actual.getLevel2();
    }
  }
}

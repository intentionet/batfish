package org.batfish.datamodel.matchers;

import javax.annotation.Nonnull;
import org.batfish.datamodel.isis.IsisHelloAuthenticationType;
import org.batfish.datamodel.isis.IsisInterfaceLevelSettings;
import org.batfish.datamodel.isis.IsisInterfaceMode;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

final class IsisInterfaceLevelSettingsMatchersImpl {
  static final class HasCost extends FeatureMatcher<IsisInterfaceLevelSettings, Long> {
    public HasCost(Matcher<? super Long> subMatcher) {
      super(subMatcher, "An IsisInterfaceLevelSettings with cost:", "cost");
    }

    @Override
    protected Long featureValueOf(IsisInterfaceLevelSettings actual) {
      return actual.getCost();
    }
  }

  static final class HasHelloAuthenticationType
      extends FeatureMatcher<IsisInterfaceLevelSettings, IsisHelloAuthenticationType> {
    public HasHelloAuthenticationType(Matcher<? super IsisHelloAuthenticationType> subMatcher) {
      super(
          subMatcher,
          "An IsisInterfaceLevelSettings with helloAuthenticationType:",
          "helloAuthenticationType");
    }

    @Override
    protected IsisHelloAuthenticationType featureValueOf(IsisInterfaceLevelSettings actual) {
      return actual.getHelloAuthenticationType();
    }
  }

  static final class HasHelloInterval extends FeatureMatcher<IsisInterfaceLevelSettings, Integer> {
    public HasHelloInterval(@Nonnull Matcher<? super Integer> subMatcher) {
      super(subMatcher, "An IsisInterfaceSettings with helloInterval:", "helloInterval");
    }

    @Override
    protected Integer featureValueOf(IsisInterfaceLevelSettings actual) {
      return actual.getHelloInterval();
    }
  }

  static final class HasHoldTime extends FeatureMatcher<IsisInterfaceLevelSettings, Integer> {
    public HasHoldTime(Matcher<? super Integer> subMatcher) {
      super(subMatcher, "An IsisInterfaceLevelSettings with holdTime:", "holdTime");
    }

    @Override
    protected Integer featureValueOf(IsisInterfaceLevelSettings actual) {
      return actual.getHoldTime();
    }
  }

  static final class HasMode extends FeatureMatcher<IsisInterfaceLevelSettings, IsisInterfaceMode> {
    public HasMode(Matcher<? super IsisInterfaceMode> subMatcher) {
      super(subMatcher, "An IsisInterfaceLevelSettings with mode:", "mode");
    }

    @Override
    protected IsisInterfaceMode featureValueOf(IsisInterfaceLevelSettings actual) {
      return actual.getMode();
    }
  }

  private IsisInterfaceLevelSettingsMatchersImpl() {}
}

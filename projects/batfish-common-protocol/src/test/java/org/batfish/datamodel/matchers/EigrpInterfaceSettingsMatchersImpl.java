package org.batfish.datamodel.matchers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.eigrp.EigrpInterfaceSettings;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

final class EigrpInterfaceSettingsMatchersImpl {
  static final class HasAsn extends FeatureMatcher<EigrpInterfaceSettings, Long> {
    public HasAsn(@Nonnull Matcher<? super Long> subMatcher) {
      super(subMatcher, "An EigrpInterfaceSettings with asn:", "asn");
    }

    @Override
    @Nullable
    protected Long featureValueOf(EigrpInterfaceSettings actual) {
      return actual.getAsNumber();
    }
  }

  static final class HasDelay extends FeatureMatcher<EigrpInterfaceSettings, Double> {
    public HasDelay(@Nonnull Matcher<? super Double> subMatcher) {
      super(subMatcher, "An EigrpInterfaceSettings with delay:", "delay");
    }

    @Override
    @Nullable
    protected Double featureValueOf(EigrpInterfaceSettings actual) {
      return actual.getDelay();
    }
  }

  static final class HasCost extends FeatureMatcher<EigrpInterfaceSettings, Long> {
    public HasCost(@Nonnull Matcher<? super Long> subMatcher) {
      super(subMatcher, "An EigrpInterfaceSettings with delay:", "delay");
    }

    @Override
    @Nullable
    protected Long featureValueOf(EigrpInterfaceSettings actual) {
      return actual.getMetric() != null ? actual.getMetric().getCost() : null;
    }
  }

  static final class HasEnabled extends FeatureMatcher<EigrpInterfaceSettings, Boolean> {
    public HasEnabled(@Nonnull Matcher<? super Boolean> subMatcher) {
      super(subMatcher, "An EigrpInterfaceSettings with enabled:", "enabled");
    }

    @Override
    protected Boolean featureValueOf(EigrpInterfaceSettings actual) {
      return actual.getEnabled();
    }
  }

  static final class HasPassive extends FeatureMatcher<EigrpInterfaceSettings, Boolean> {
    public HasPassive(@Nonnull Matcher<? super Boolean> subMatcher) {
      super(subMatcher, "An EigrpInterfaceSettings with passive:", "passive");
    }

    @Override
    protected Boolean featureValueOf(EigrpInterfaceSettings actual) {
      return actual.getPassive();
    }
  }
}

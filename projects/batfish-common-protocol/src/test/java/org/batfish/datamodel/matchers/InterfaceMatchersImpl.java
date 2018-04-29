package org.batfish.datamodel.matchers;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.OspfArea;
import org.batfish.datamodel.SourceNat;
import org.batfish.datamodel.Vrf;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

final class InterfaceMatchersImpl {

  static final class HasAdditionalArpIps extends FeatureMatcher<Interface, SortedSet<Ip>> {
    HasAdditionalArpIps(@Nonnull Matcher<? super SortedSet<Ip>> subMatcher) {
      super(subMatcher, "An interface with additionalArpIps:", "additionalArpIps");
    }

    @Override
    protected SortedSet<Ip> featureValueOf(Interface actual) {
      return actual.getAdditionalArpIps();
    }
  }

  static final class HasDeclaredNames extends FeatureMatcher<Interface, Set<String>> {
    HasDeclaredNames(@Nonnull Matcher<? super Set<String>> subMatcher) {
      super(subMatcher, "declared names", "declared names");
    }

    @Override
    protected Set<String> featureValueOf(Interface actual) {
      return actual.getDeclaredNames();
    }
  }

  static final class HasMtu extends FeatureMatcher<Interface, Integer> {
    HasMtu(@Nonnull Matcher<? super Integer> subMatcher) {
      super(subMatcher, "an Interface with MTU:", "MTU");
    }

    @Override
    protected Integer featureValueOf(Interface actual) {
      return actual.getMtu();
    }
  }

  static final class HasOspfArea extends FeatureMatcher<Interface, OspfArea> {
    HasOspfArea(@Nonnull Matcher<? super OspfArea> subMatcher) {
      super(subMatcher, "an Interface with ospfArea:", "ospfArea");
    }

    @Override
    protected OspfArea featureValueOf(Interface actual) {
      return actual.getOspfArea();
    }
  }

  static final class HasOspfCost extends FeatureMatcher<Interface, Integer> {
    HasOspfCost(@Nonnull Matcher<? super Integer> subMatcher) {
      super(subMatcher, "an Interface with ospfCost:", "ospfCost");
    }

    @Override
    protected Integer featureValueOf(Interface actual) {
      return actual.getOspfCost();
    }
  }

  static final class HasOutgoingFilterName extends FeatureMatcher<Interface, String> {
    HasOutgoingFilterName(@Nonnull Matcher<? super String> subMatcher) {
      super(subMatcher, "an Interface with outgoingFilterName:", "outgoingFilterName");
    }

    @Override
    protected String featureValueOf(Interface actual) {
      return actual.getOutgoingFilterName();
    }
  }

  static final class HasSourceNats extends FeatureMatcher<Interface, List<SourceNat>> {
    HasSourceNats(@Nonnull Matcher<? super List<SourceNat>> subMatcher) {
      super(subMatcher, "an Interface with sourceNats:", "sourceNats");
    }

    @Override
    protected List<SourceNat> featureValueOf(Interface actual) {
      return actual.getSourceNats();
    }
  }

  static final class HasVrf extends FeatureMatcher<Interface, Vrf> {
    HasVrf(@Nonnull Matcher<? super Vrf> subMatcher) {
      super(subMatcher, "an Interface with vrf:", "vrf");
    }

    @Override
    protected Vrf featureValueOf(Interface actual) {
      return actual.getVrf();
    }
  }

  static final class HasZoneName extends FeatureMatcher<Interface, String> {
    HasZoneName(@Nonnull Matcher<? super String> subMatcher) {
      super(subMatcher, "an Interface with zoneName:", "zoneName");
    }

    @Override
    protected String featureValueOf(Interface actual) {
      return actual.getZoneName();
    }
  }

  static final class IsActive extends FeatureMatcher<Interface, Boolean> {
    IsActive(@Nonnull Matcher<? super Boolean> subMatcher) {
      super(subMatcher, "an Interface with active:", "active");
    }

    @Override
    protected Boolean featureValueOf(Interface actual) {
      return actual.getActive();
    }
  }

  static final class IsOspfPassive extends FeatureMatcher<Interface, Boolean> {
    IsOspfPassive(@Nonnull Matcher<? super Boolean> subMatcher) {
      super(subMatcher, "an Interface with ospfPassive:", "ospfPassive");
    }

    @Override
    protected Boolean featureValueOf(Interface actual) {
      return actual.getOspfPassive();
    }
  }

  static final class IsOspfPointToPoint extends FeatureMatcher<Interface, Boolean> {
    IsOspfPointToPoint(@Nonnull Matcher<? super Boolean> subMatcher) {
      super(subMatcher, "an Interface with ospfPointToPoint:", "ospfPointToPoint");
    }

    @Override
    protected Boolean featureValueOf(Interface actual) {
      return actual.getOspfPointToPoint();
    }
  }

  static final class IsProxyArp extends FeatureMatcher<Interface, Boolean> {
    IsProxyArp(@Nonnull Matcher<? super Boolean> subMatcher) {
      super(subMatcher, "an Interface with proxyArp:", "proxyArp");
    }

    @Override
    protected Boolean featureValueOf(Interface actual) {
      return actual.getProxyArp();
    }
  }

  private InterfaceMatchersImpl() {}
}

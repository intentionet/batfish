package org.batfish.bddreachability;

import java.util.Set;
import javax.annotation.Nonnull;
import net.sf.javabdd.BDD;
import org.batfish.bddreachability.transition.Transition;
import org.batfish.datamodel.flow.SessionAction;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public class BDDFirewallSessionTraceInfoMatchersImpl {

  static final class HasAction extends FeatureMatcher<BDDFirewallSessionTraceInfo, SessionAction> {
    public HasAction(Matcher<? super SessionAction> subMatcher) {
      super(subMatcher, "A BDDFirewallSessionTraceInfo with action:", "action");
    }

    @Override
    protected @Nonnull SessionAction featureValueOf(
        BDDFirewallSessionTraceInfo bddFirewallSessionTraceInfo) {
      return bddFirewallSessionTraceInfo.getAction();
    }
  }

  static final class HasHostname extends FeatureMatcher<BDDFirewallSessionTraceInfo, String> {
    public HasHostname(Matcher<? super String> subMatcher) {
      super(subMatcher, "A BDDFirewallSessionTraceInfo with hostname:", "hostname");
    }

    @Override
    protected String featureValueOf(BDDFirewallSessionTraceInfo bddFirewallSessionTraceInfo) {
      return bddFirewallSessionTraceInfo.getHostname();
    }
  }

  static final class HasIncomingInterfaces
      extends FeatureMatcher<BDDFirewallSessionTraceInfo, Set<String>> {
    public HasIncomingInterfaces(Matcher<? super Set<String>> subMatcher) {
      super(
          subMatcher,
          "A BDDFirewallSessionTraceInfo with incomingInterfaces:",
          "incomingInterfaces");
    }

    @Override
    protected Set<String> featureValueOf(BDDFirewallSessionTraceInfo bddFirewallSessionTraceInfo) {
      return bddFirewallSessionTraceInfo.getIncomingInterfaces();
    }
  }

  static final class HasSessionFlows extends FeatureMatcher<BDDFirewallSessionTraceInfo, BDD> {
    public HasSessionFlows(Matcher<? super BDD> subMatcher) {
      super(subMatcher, "A BDDFirewallSessionTraceInfo with sessionFlows:", "sessionFlows");
    }

    @Override
    protected BDD featureValueOf(BDDFirewallSessionTraceInfo bddFirewallSessionTraceInfo) {
      return bddFirewallSessionTraceInfo.getSessionFlows();
    }
  }

  static final class HasTransformation
      extends FeatureMatcher<BDDFirewallSessionTraceInfo, Transition> {
    public HasTransformation(Matcher<? super Transition> subMatcher) {
      super(subMatcher, "A BDDFirewallSessionTraceInfo with transformation:", "transformation");
    }

    @Override
    protected Transition featureValueOf(BDDFirewallSessionTraceInfo bddFirewallSessionTraceInfo) {
      return bddFirewallSessionTraceInfo.getTransformation();
    }
  }
}

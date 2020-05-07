package org.batfish.bddreachability;

import static org.hamcrest.Matchers.equalTo;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import net.sf.javabdd.BDD;
import org.batfish.bddreachability.BDDFirewallSessionTraceInfoMatchersImpl.HasAction;
import org.batfish.bddreachability.BDDFirewallSessionTraceInfoMatchersImpl.HasHostname;
import org.batfish.bddreachability.BDDFirewallSessionTraceInfoMatchersImpl.HasIncomingInterfaces;
import org.batfish.bddreachability.BDDFirewallSessionTraceInfoMatchersImpl.HasSessionFlows;
import org.batfish.bddreachability.BDDFirewallSessionTraceInfoMatchersImpl.HasTransformation;
import org.batfish.bddreachability.transition.Transition;
import org.batfish.datamodel.flow.SessionAction;
import org.hamcrest.Matcher;

/** Matchers for {@link BDDFirewallSessionTraceInfo}. */
@ParametersAreNonnullByDefault
public final class BDDFirewallSessionTraceInfoMatchers {
  private BDDFirewallSessionTraceInfoMatchers() {}

  public static @Nonnull Matcher<BDDFirewallSessionTraceInfo> hasAction(
      SessionAction expectedAction) {
    return hasAction(equalTo(expectedAction));
  }

  public static @Nonnull Matcher<BDDFirewallSessionTraceInfo> hasAction(
      Matcher<? super SessionAction> subMatcher) {
    return new HasAction(subMatcher);
  }

  public static HasHostname hasHostname(String hostname) {
    return new HasHostname(equalTo(hostname));
  }

  /**
   * Matches {@link BDDFirewallSessionTraceInfo} that can match flows entering a set of interfaces
   * that match the given matcher
   */
  public static HasIncomingInterfaces hasIncomingInterfaces(Matcher<? super Set<String>> matcher) {
    return new HasIncomingInterfaces(matcher);
  }

  public static HasSessionFlows hasSessionFlows(BDD bdd) {
    return new HasSessionFlows(equalTo(bdd));
  }

  public static HasTransformation hasTransformation(Transition transformation) {
    return new HasTransformation(equalTo(transformation));
  }
}

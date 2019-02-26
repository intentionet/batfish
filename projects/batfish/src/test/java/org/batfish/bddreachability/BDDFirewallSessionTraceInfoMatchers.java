package org.batfish.bddreachability;

import static org.hamcrest.Matchers.equalTo;

import java.util.Set;
import net.sf.javabdd.BDD;
import org.batfish.bddreachability.BDDFirewallSessionTraceInfoMatchersImpl.HasHostname;
import org.batfish.bddreachability.BDDFirewallSessionTraceInfoMatchersImpl.HasIncomingInterfaces;
import org.batfish.bddreachability.BDDFirewallSessionTraceInfoMatchersImpl.HasNextHop;
import org.batfish.bddreachability.BDDFirewallSessionTraceInfoMatchersImpl.HasOutgoingInterface;
import org.batfish.bddreachability.BDDFirewallSessionTraceInfoMatchersImpl.HasSessionFlows;
import org.batfish.bddreachability.BDDFirewallSessionTraceInfoMatchersImpl.HasTransformation;
import org.batfish.bddreachability.transition.Transition;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.hamcrest.Matcher;

public final class BDDFirewallSessionTraceInfoMatchers {
  private BDDFirewallSessionTraceInfoMatchers() {}

  public static HasHostname hasHostname(String hostname) {
    return new HasHostname(equalTo(hostname));
  }

  public static HasIncomingInterfaces hasIncomingInterfaces(Matcher<? super Set<String>> matcher) {
    return new HasIncomingInterfaces(matcher);
  }

  public static HasNextHop hasNextHop(NodeInterfacePair nodeInterfacePair) {
    return new HasNextHop(equalTo(nodeInterfacePair));
  }

  public static HasNextHop hasNextHop(Matcher<? super NodeInterfacePair> matcher) {
    return new HasNextHop(matcher);
  }

  public static HasOutgoingInterface hasOutgoingInterface(String outIface) {
    return new HasOutgoingInterface(equalTo(outIface));
  }

  public static HasSessionFlows hasSessionFlows(BDD bdd) {
    return new HasSessionFlows(equalTo(bdd));
  }

  public static HasTransformation hasTransformation(Transition transformation) {
    return new HasTransformation(equalTo(transformation));
  }
}

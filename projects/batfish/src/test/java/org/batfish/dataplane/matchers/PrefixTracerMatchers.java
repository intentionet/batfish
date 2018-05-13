package org.batfish.dataplane.matchers;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.collection.IsMapContaining.hasEntry;

import java.util.Map;
import java.util.Set;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.dataplane.ibdp.PrefixTracer;
import org.batfish.dataplane.ibdp.PrefixTracer.Neighbor;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

/** A set of matchers for testing {@link PrefixTracer} */
public final class PrefixTracerMatchers {

  static final class FilteredOut extends FeatureMatcher<PrefixTracer, Map<Prefix, Set<Neighbor>>> {

    FilteredOut(Matcher<? super Map<Prefix, Set<Neighbor>>> subMatcher) {
      super(subMatcher, "Filtered outbound:", "filtered routes");
    }

    @Override
    protected Map<Prefix, Set<Neighbor>> featureValueOf(PrefixTracer prefixTracer) {
      return prefixTracer.getFiltered(Direction.OUT);
    }
  }

  static final class FilteredIn extends FeatureMatcher<PrefixTracer, Map<Prefix, Set<Neighbor>>> {

    FilteredIn(Matcher<? super Map<Prefix, Set<Neighbor>>> subMatcher) {
      super(subMatcher, "Filtered inbound:", "filtered routes");
    }

    @Override
    protected Map<Prefix, Set<Neighbor>> featureValueOf(PrefixTracer prefixTracer) {
      return prefixTracer.getFiltered(Direction.IN);
    }
  }

  static final class WasSent extends FeatureMatcher<PrefixTracer, Map<Prefix, Set<Neighbor>>> {
    WasSent(Matcher<? super Map<Prefix, Set<Neighbor>>> subMatcher) {
      super(subMatcher, "Sent routes:", "sent routes");
    }

    @Override
    protected Map<Prefix, Set<Neighbor>> featureValueOf(PrefixTracer prefixTracer) {
      return prefixTracer.getSent();
    }
  }

  static final class WasInstalled extends FeatureMatcher<PrefixTracer, Map<Prefix, Set<Neighbor>>> {

    public WasInstalled(Matcher<? super Map<Prefix, Set<Neighbor>>> subMatcher) {
      super(subMatcher, "Installed routes:", "installed routes");
    }

    @Override
    protected Map<Prefix, Set<Neighbor>> featureValueOf(PrefixTracer prefixTracer) {
      return prefixTracer.getInstalled();
    }
  }

  static final class HasHostname extends FeatureMatcher<Neighbor, String> {

    HasHostname(Matcher<? super String> subMatcher) {
      super(subMatcher, "Neighbor with hostname:", "hostname");
    }

    @Override
    protected String featureValueOf(Neighbor neighbor) {
      return neighbor.getHostname();
    }
  }

  /** Matches if a matched by the submatcher prefix was sent */
  public static WasSent wasSent(Matcher<? super Map<Prefix, Set<Neighbor>>> submatcher) {
    return new WasSent(submatcher);
  }

  /** Matches if a prefix was received from neighbors specified by the submatcher and installed */
  public static WasInstalled wasInstalled(
      Prefix prefix, Matcher<? super Set<Neighbor>> submatcher) {
    return new WasInstalled(hasEntry(equalTo(prefix), submatcher));
  }

  /** Matches if a prefix was sent to any neighbors specified by the submatcher */
  public static WasSent wasSent(Prefix prefix, Matcher<? super Set<Neighbor>> submatcher) {
    return new WasSent(hasEntry(equalTo(prefix), submatcher));
  }

  /** Matches if a prefix was filtered on inbound at a neighbor specified by the submatcher */
  public static FilteredIn wasFilteredIn(Prefix prefix, Matcher<? super Set<Neighbor>> submatcher) {
    return new FilteredIn(hasEntry(equalTo(prefix), submatcher));
  }

  /** Matches if a prefix was filtered on outbound at a neighbor specified by the submatcher */
  public static FilteredOut wasFilteredOut(
      Prefix prefix, Matcher<? super Set<Neighbor>> submatcher) {
    return new FilteredOut(hasEntry(equalTo(prefix), submatcher));
  }

  /** Matches if a neighbor has a specified hostname */
  public static HasHostname hasHostname(String hostname) {
    return new HasHostname(equalTo(hostname));
  }

  /** Matches if any of the neighbors have a specified hostname */
  public static Matcher<? super Iterable<Neighbor>> toHostname(String hostname) {
    return hasItem(new HasHostname(equalTo(hostname)));
  }

  /** Matches if any of the neighbors have a specified hostname */
  public static Matcher<? super Iterable<Neighbor>> fromHostname(String hostname) {
    return hasItem(new HasHostname(equalTo(hostname)));
  }
}

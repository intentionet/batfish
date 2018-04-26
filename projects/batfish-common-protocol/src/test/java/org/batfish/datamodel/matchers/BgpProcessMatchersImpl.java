package org.batfish.datamodel.matchers;

import java.util.Map;
import javax.annotation.Nonnull;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

final class BgpProcessMatchersImpl {

  static final class HasNeighbor extends FeatureMatcher<BgpProcess, BgpNeighbor> {
    private final Prefix _prefix;

    HasNeighbor(@Nonnull Prefix prefix, @Nonnull Matcher<? super BgpNeighbor> subMatcher) {
      super(subMatcher, "A BGP process with neighbor " + prefix + ":", "neighbor " + prefix);
      _prefix = prefix;
    }

    @Override
    protected BgpNeighbor featureValueOf(BgpProcess actual) {
      return actual.getNeighbors().get(_prefix);
    }
  }

  static final class HasNeighbors extends FeatureMatcher<BgpProcess, Map<Prefix, BgpNeighbor>> {
    HasNeighbors(@Nonnull Matcher<? super Map<Prefix, BgpNeighbor>> subMatcher) {
      super(subMatcher, "A BGP process with neighbors:", "neighbors");
    }

    @Override
    protected Map<Prefix, BgpNeighbor> featureValueOf(BgpProcess actual) {
      return actual.getNeighbors();
    }
  }

  static final class HasRouterId extends FeatureMatcher<BgpProcess, Ip> {
    HasRouterId(@Nonnull Matcher<? super Ip> subMatcher) {
      super(subMatcher, "A BGP process with router id:", "router id");
    }

    @Override
    protected Ip featureValueOf(BgpProcess actual) {
      return actual.getRouterId();
    }
  }
}

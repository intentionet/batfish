package org.batfish.datamodel.matchers;

import java.util.SortedSet;
import javax.annotation.Nonnull;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.isis.IsisProcess;
import org.batfish.datamodel.ospf.OspfProcess;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

final class VrfMatchersImpl {

  static final class HasBgpProcess extends FeatureMatcher<Vrf, BgpProcess> {
    HasBgpProcess(@Nonnull Matcher<? super BgpProcess> subMatcher) {
      super(subMatcher, "A VRF with bgpProcess:", "bgpProcess");
    }

    @Override
    protected BgpProcess featureValueOf(Vrf actual) {
      return actual.getBgpProcess();
    }
  }

  static final class HasInterfaces extends FeatureMatcher<Vrf, SortedSet<String>> {
    HasInterfaces(@Nonnull Matcher<? super SortedSet<String>> subMatcher) {
      super(subMatcher, "A VRF with interfaces:", "interfaces");
    }

    @Override
    protected SortedSet<String> featureValueOf(Vrf actual) {
      return actual.getInterfaceNames();
    }
  }

  static final class HasIsisProcess extends FeatureMatcher<Vrf, IsisProcess> {
    HasIsisProcess(@Nonnull Matcher<? super IsisProcess> subMatcher) {
      super(subMatcher, "A Vrf with isisProcess:", "isisProcess");
    }

    @Override
    protected IsisProcess featureValueOf(Vrf actual) {
      return actual.getIsisProcess();
    }
  }

  static final class HasOspfProcess extends FeatureMatcher<Vrf, OspfProcess> {
    HasOspfProcess(@Nonnull Matcher<? super OspfProcess> subMatcher) {
      super(subMatcher, "A VRF with ospfProcess:", "ospfProcess");
    }

    @Override
    protected OspfProcess featureValueOf(Vrf actual) {
      return actual.getOspfProcess();
    }
  }

  static final class HasStaticRoutes extends FeatureMatcher<Vrf, SortedSet<StaticRoute>> {
    HasStaticRoutes(@Nonnull Matcher<? super SortedSet<StaticRoute>> subMatcher) {
      super(subMatcher, "A VRF with staticRoutes:", "staticRoutes");
    }

    @Override
    protected SortedSet<StaticRoute> featureValueOf(Vrf actual) {
      return actual.getStaticRoutes();
    }
  }
}

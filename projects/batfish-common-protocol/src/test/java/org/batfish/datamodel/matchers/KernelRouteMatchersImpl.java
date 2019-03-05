package org.batfish.datamodel.matchers;

import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.KernelRoute;
import org.hamcrest.Matcher;

@ParametersAreNonnullByDefault
final class KernelRouteMatchersImpl {
  static final class IsKernelRouteThat extends IsInstanceThat<AbstractRoute, KernelRoute> {
    IsKernelRouteThat(Matcher<? super KernelRoute> subMatcher) {
      super(KernelRoute.class, subMatcher);
    }
  }

  private KernelRouteMatchersImpl() {}
}

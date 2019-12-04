package org.batfish.datamodel.matchers;

import java.util.Map;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.KernelRoute;
import org.batfish.datamodel.SnmpServer;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.eigrp.EigrpProcess;
import org.batfish.datamodel.isis.IsisProcess;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.vxlan.Layer2Vni;
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

  static final class HasEigrpProcesses extends FeatureMatcher<Vrf, Map<Long, EigrpProcess>> {
    HasEigrpProcesses(@Nonnull Matcher<? super Map<Long, EigrpProcess>> subMatcher) {
      super(subMatcher, "A Vrf with eigrpProcesses:", "eigrpProcesses");
    }

    @Override
    protected Map<Long, EigrpProcess> featureValueOf(Vrf actual) {
      return actual.getEigrpProcesses();
    }
  }

  static final class HasGeneratedRoutes extends FeatureMatcher<Vrf, SortedSet<GeneratedRoute>> {
    HasGeneratedRoutes(@Nonnull Matcher<? super SortedSet<GeneratedRoute>> subMatcher) {
      super(subMatcher, "A VRF with generatedRoutes:", "generatedRoutes");
    }

    @Override
    protected SortedSet<GeneratedRoute> featureValueOf(Vrf actual) {
      return actual.getGeneratedRoutes();
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

  static final class HasKernelRoutes extends FeatureMatcher<Vrf, SortedSet<KernelRoute>> {
    HasKernelRoutes(@Nonnull Matcher<? super SortedSet<KernelRoute>> subMatcher) {
      super(subMatcher, "A VRF with kernelRoutes:", "kernelRoutes");
    }

    @Override
    protected SortedSet<KernelRoute> featureValueOf(Vrf actual) {
      return actual.getKernelRoutes();
    }
  }

  static final class HasOspfProcesses extends FeatureMatcher<Vrf, Map<String, OspfProcess>> {
    HasOspfProcesses(@Nonnull Matcher<? super Map<String, OspfProcess>> subMatcher) {
      super(subMatcher, "A VRF with ospfProcesses:", "ospfProcesses");
    }

    @Override
    protected Map<String, OspfProcess> featureValueOf(Vrf actual) {
      return actual.getOspfProcesses();
    }
  }

  static final class HasName extends FeatureMatcher<Vrf, String> {
    HasName(@Nonnull Matcher<? super String> subMatcher) {
      super(subMatcher, "A VRF with name:", "name");
    }

    @Override
    protected String featureValueOf(Vrf actual) {
      return actual.getName();
    }
  }

  static final class HasSnmpServer extends FeatureMatcher<Vrf, SnmpServer> {
    HasSnmpServer(@Nonnull Matcher<? super SnmpServer> subMatcher) {
      super(subMatcher, "A Vrf with snmpServer:", "snmpServer");
    }

    @Override
    protected SnmpServer featureValueOf(Vrf actual) {
      return actual.getSnmpServer();
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

  static final class HasVniSettings extends FeatureMatcher<Vrf, Map<Integer, Layer2Vni>> {
    HasVniSettings(@Nonnull Matcher<? super Map<Integer, Layer2Vni>> subMatcher) {
      super(subMatcher, "A VRF with VNI settings:", "vniSettings");
    }

    @Override
    protected Map<Integer, Layer2Vni> featureValueOf(Vrf actual) {
      return actual.getLayer2Vnis();
    }
  }
}

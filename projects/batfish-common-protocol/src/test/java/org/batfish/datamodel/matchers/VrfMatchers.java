package org.batfish.datamodel.matchers;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;

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
import org.batfish.datamodel.matchers.VrfMatchersImpl.HasBgpProcess;
import org.batfish.datamodel.matchers.VrfMatchersImpl.HasEigrpProcesses;
import org.batfish.datamodel.matchers.VrfMatchersImpl.HasGeneratedRoutes;
import org.batfish.datamodel.matchers.VrfMatchersImpl.HasInterfaces;
import org.batfish.datamodel.matchers.VrfMatchersImpl.HasKernelRoutes;
import org.batfish.datamodel.matchers.VrfMatchersImpl.HasName;
import org.batfish.datamodel.matchers.VrfMatchersImpl.HasOspfProcesses;
import org.batfish.datamodel.matchers.VrfMatchersImpl.HasSnmpServer;
import org.batfish.datamodel.matchers.VrfMatchersImpl.HasStaticRoutes;
import org.batfish.datamodel.matchers.VrfMatchersImpl.HasVniSettings;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.vxlan.Layer2Vni;
import org.hamcrest.Matcher;

public class VrfMatchers {

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the VRF's BGP
   * process.
   */
  public static HasBgpProcess hasBgpProcess(Matcher<? super BgpProcess> subMatcher) {
    return new HasBgpProcess(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the VRF's EIGRP
   * process.
   */
  public static HasEigrpProcesses hasEigrpProcesses(
      Matcher<? super Map<Long, EigrpProcess>> subMatcher) {
    return new HasEigrpProcesses(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the VRF's generated
   * routes.
   */
  public static HasGeneratedRoutes hasGeneratedRoutes(
      Matcher<? super SortedSet<GeneratedRoute>> subMatcher) {
    return new HasGeneratedRoutes(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the VRF's
   * interfaces.
   */
  public static Matcher<Vrf> hasInterfaces(Matcher<? super SortedSet<String>> subMatcher) {
    return new HasInterfaces(subMatcher);
  }

  /**
   * Provides a matcher that matches if the VRF has an OSPF process named {@code name} that matches
   * the provided {@code subMatcher}.
   */
  public static HasOspfProcesses hasOspfProcess(
      String name, Matcher<? super OspfProcess> subMatcher) {
    return new HasOspfProcesses(hasEntry(equalTo(name), subMatcher));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the VRF's kernel
   * routes.
   */
  public static Matcher<Vrf> hasKernelRoutes(Matcher<? super SortedSet<KernelRoute>> subMatcher) {
    return new HasKernelRoutes(subMatcher);
  }

  /** Provides a matcher that matches if the provided {@code subMatcher} matches the VRF's name. */
  public static HasName hasName(Matcher<? super String> subMatcher) {
    return new HasName(subMatcher);
  }

  public static @Nonnull Matcher<Vrf> hasSnmpServer(
      @Nonnull Matcher<? super SnmpServer> subMatcher) {
    return new HasSnmpServer(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the VRF's static
   * routes.
   */
  public static HasStaticRoutes hasStaticRoutes(
      Matcher<? super SortedSet<StaticRoute>> subMatcher) {
    return new HasStaticRoutes(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the VRF's VXLAN VNI
   * settings.
   */
  public static HasVniSettings hasL2VniSettings(
      Matcher<? super Map<Integer, Layer2Vni>> subMatcher) {
    return new HasVniSettings(subMatcher);
  }
}

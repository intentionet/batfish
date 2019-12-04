package org.batfish.datamodel.matchers;

import javax.annotation.Nonnull;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.vxlan.Layer2Vni;
import org.batfish.datamodel.vxlan.Vni;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public class VniSettingsMatchersImpl {

  private VniSettingsMatchersImpl() {}

  static final class HasBumTransportIps extends FeatureMatcher<Vni, Iterable<Ip>> {
    HasBumTransportIps(@Nonnull Matcher<? super Iterable<Ip>> subMatcher) {
      super(subMatcher, "Layer2Vni with BUM transport IPs:", "bumTransportIps");
    }

    @Override
    protected Iterable<Ip> featureValueOf(Vni actual) {
      return actual.getBumTransportIps();
    }
  }

  static final class HasBumTransportMethod extends FeatureMatcher<Vni, BumTransportMethod> {
    HasBumTransportMethod(@Nonnull Matcher<? super BumTransportMethod> subMatcher) {
      super(subMatcher, "Layer2Vni with BUM transport method:", "bumTransportMethod");
    }

    @Override
    protected BumTransportMethod featureValueOf(Vni actual) {
      return actual.getBumTransportMethod();
    }
  }

  static final class HasSourceAddress extends FeatureMatcher<Vni, Ip> {
    HasSourceAddress(@Nonnull Matcher<? super Ip> subMatcher) {
      super(subMatcher, "Layer2Vni with source IP address:", "sourceAddress");
    }

    @Override
    protected Ip featureValueOf(Vni actual) {
      return actual.getSourceAddress();
    }
  }

  static final class HasUdpPort extends FeatureMatcher<Vni, Integer> {
    HasUdpPort(@Nonnull Matcher<? super Integer> subMatcher) {
      super(subMatcher, "Layer2Vni with UDP port:", "udpPort");
    }

    @Override
    protected Integer featureValueOf(Vni actual) {
      return actual.getUdpPort();
    }
  }

  static final class HasVlan extends FeatureMatcher<Layer2Vni, Integer> {
    HasVlan(@Nonnull Matcher<? super Integer> subMatcher) {
      super(subMatcher, "Layer2Vni with VLAN number:", "vlan");
    }

    @Override
    protected Integer featureValueOf(Layer2Vni actual) {
      return actual.getVlan();
    }
  }

  static final class HasVni extends FeatureMatcher<Vni, Integer> {
    HasVni(@Nonnull Matcher<? super Integer> subMatcher) {
      super(subMatcher, "Layer2Vni with VLAN number:", "vlan");
    }

    @Override
    protected Integer featureValueOf(Vni actual) {
      return actual.getVni();
    }
  }
}

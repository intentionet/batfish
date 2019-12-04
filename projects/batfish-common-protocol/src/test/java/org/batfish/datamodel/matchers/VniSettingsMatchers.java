package org.batfish.datamodel.matchers;

import static org.hamcrest.Matchers.equalTo;

import javax.annotation.Nonnull;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.matchers.VniSettingsMatchersImpl.HasBumTransportIps;
import org.batfish.datamodel.matchers.VniSettingsMatchersImpl.HasBumTransportMethod;
import org.batfish.datamodel.matchers.VniSettingsMatchersImpl.HasSourceAddress;
import org.batfish.datamodel.matchers.VniSettingsMatchersImpl.HasUdpPort;
import org.batfish.datamodel.matchers.VniSettingsMatchersImpl.HasVlan;
import org.batfish.datamodel.matchers.VniSettingsMatchersImpl.HasVni;
import org.batfish.datamodel.vxlan.Layer2Vni;
import org.batfish.datamodel.vxlan.Vni;
import org.hamcrest.Matcher;

public class VniSettingsMatchers {

  private VniSettingsMatchers() {}

  /**
   * Provides a matcher that matches if the {@link Vni}'s BUM transport IPs matches the {@code
   * subMatcher}.
   */
  public static @Nonnull Matcher<Vni> hasBumTransportIps(
      @Nonnull Matcher<? super Iterable<Ip>> subMatcher) {
    return new HasBumTransportIps(subMatcher);
  }

  /**
   * Provides a matcher that matches if the {@link Vni}'s BUM transport method matches the {@code
   * subMatcher}.
   */
  public static @Nonnull Matcher<Vni> hasBumTransportMethod(
      @Nonnull Matcher<? super BumTransportMethod> subMatcher) {
    return new HasBumTransportMethod(subMatcher);
  }

  /**
   * Provides a matcher that matches if the {@link Vni}'s source IP address matches the {@code
   * subMatcher}.
   */
  public static @Nonnull Matcher<Vni> hasSourceAddress(@Nonnull Matcher<? super Ip> subMatcher) {
    return new HasSourceAddress(subMatcher);
  }

  /**
   * Provides a matcher that matches if the {@link Vni}'s UDP port matches the {@code subMatcher}.
   */
  public static @Nonnull Matcher<Vni> hasUdpPort(@Nonnull Matcher<? super Integer> subMatcher) {
    return new HasUdpPort(subMatcher);
  }

  /**
   * Provides a matcher that matches if the {@link Vni}'s VLAN number matches the {@code
   * subMatcher}.
   */
  public static @Nonnull Matcher<Layer2Vni> hasVlan(@Nonnull Matcher<? super Integer> subMatcher) {
    return new HasVlan(subMatcher);
  }

  /** Provides a matcher that matches if the {@link Vni}'s VLAN number matches the {@code vni}. */
  public static @Nonnull Matcher<Vni> hasVni(int vni) {
    return new HasVni(equalTo(vni));
  }
}

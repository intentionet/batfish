package org.batfish.matchers;

import static org.hamcrest.Matchers.equalTo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.dataplane.rib.RouteAdvertisement;
import org.batfish.dataplane.rib.RouteAdvertisement.Reason;
import org.batfish.matchers.RouteAdvertisementMatchersImpl.HasNextHopIp;
import org.batfish.matchers.RouteAdvertisementMatchersImpl.HasPrefix;
import org.batfish.matchers.RouteAdvertisementMatchersImpl.HasReason;

/** Matchers for {@link RouteAdvertisement} */
@ParametersAreNonnullByDefault
public class RouteAdvertisementMatchers {
  /**
   * Provides a matcher that matches when the supplied {@link Ip} is equal to the {@link
   * RouteAdvertisement}'s nextHopIp.
   */
  public static @Nonnull HasNextHopIp hasNextHopIp(@Nullable Ip ip) {
    return new HasNextHopIp(equalTo(ip));
  }

  /**
   * Provides a matcher that matches when the {@code expectedPrefix} is equal to the {@link
   * RouteAdvertisement}'s prefix.
   */
  public static @Nonnull HasPrefix hasPrefix(Prefix expectedPrefix) {
    return new HasPrefix(equalTo(expectedPrefix));
  }

  /**
   * Provides a matcher that matches when the {@code reason} is equal to the {@link
   * RouteAdvertisement}'s reason.
   */
  public static @Nonnull HasReason hasReason(Reason reason) {
    return new HasReason(equalTo(reason));
  }
}

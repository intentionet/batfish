package org.batfish.datamodel.matchers;

import static org.hamcrest.Matchers.equalTo;

import java.util.Map;
import javax.annotation.Nonnull;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.matchers.BgpProcessMatchersImpl.HasNeighbor;
import org.batfish.datamodel.matchers.BgpProcessMatchersImpl.HasNeighbors;
import org.batfish.datamodel.matchers.BgpProcessMatchersImpl.HasRouterId;
import org.hamcrest.Matcher;

/** {@link Matcher Hamcrest matchers} for {@link BgpProcess}. */
public class BgpProcessMatchers {

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the BGP process's
   * neighbor with specified prefix.
   */
  public static HasNeighbor hasNeighbor(
      @Nonnull Prefix prefix, @Nonnull Matcher<? super BgpNeighbor> subMatcher) {
    return new HasNeighbor(prefix, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the BGP process's
   * neighbors.
   */
  public static HasNeighbors hasNeighbors(
      @Nonnull Matcher<? super Map<Prefix, BgpNeighbor>> subMatcher) {
    return new HasNeighbors(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the BGP process's
   * router id.
   */
  public static HasRouterId hasRouterId(@Nonnull Matcher<? super Ip> subMatcher) {
    return new HasRouterId(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code ip} matches the BGP process's router id.
   */
  public static HasRouterId hasRouterId(Ip ip) {
    return new HasRouterId(equalTo(ip));
  }
}

package org.batfish.datamodel.matchers;

import static org.hamcrest.Matchers.equalTo;

import org.batfish.datamodel.bgp.AddressFamilyCapabilities;
import org.batfish.datamodel.matchers.AddressFamilyCapabilitiesMatchersImpl.HasAllowLocalAsIn;
import org.batfish.datamodel.matchers.AddressFamilyCapabilitiesMatchersImpl.HasAllowRemoteAsOut;
import org.batfish.datamodel.matchers.AddressFamilyCapabilitiesMatchersImpl.HasSendCommunity;
import org.hamcrest.Matcher;

/** Matchers for {@link AddressFamilyCapabilities} */
public final class AddressFamilyCapabilitiesMatchers {

  /**
   * Provides a matcher that matches if the {@link AddressFamilyCapabilities}'s allowLocalAsIn is
   * {@code value}.
   */
  public static HasAllowLocalAsIn hasAllowLocalAsIn(boolean value) {
    return new HasAllowLocalAsIn(equalTo(value));
  }

  /**
   * Provides a matcher that matches if the {@link AddressFamilyCapabilities}'s allowRemoteAsOut is
   * {@code value}.
   */
  public static HasAllowRemoteAsOut hasAllowRemoteAsOut(boolean value) {
    return new HasAllowRemoteAsOut(equalTo(value));
  }

  /**
   * Provides a matcher that matches if the {@link AddressFamilyCapabilities}'s sendCommunity is
   * equal to the given value.
   */
  public static Matcher<AddressFamilyCapabilities> hasSendCommunity(boolean value) {
    return new HasSendCommunity(equalTo(value));
  }

  private AddressFamilyCapabilitiesMatchers() {}
}

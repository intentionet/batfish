package org.batfish.datamodel.vendor_family.cisco;

import static org.hamcrest.Matchers.equalTo;

import org.batfish.datamodel.vendor_family.cisco.LoggingMatchersImpl.HasOn;

public class LoggingMatchers {

  /** Provides a matcher that matches if logging is on. */
  public static HasOn isOn() {
    return new HasOn(equalTo(true));
  }

  private LoggingMatchers() {}
}

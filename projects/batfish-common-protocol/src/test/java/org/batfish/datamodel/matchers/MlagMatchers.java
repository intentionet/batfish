package org.batfish.datamodel.matchers;

import static org.hamcrest.Matchers.equalTo;

import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Mlag;
import org.batfish.datamodel.matchers.MlagMatchersImpl.HasId;
import org.batfish.datamodel.matchers.MlagMatchersImpl.HasLocalInterface;
import org.batfish.datamodel.matchers.MlagMatchersImpl.HasPeerAddress;
import org.batfish.datamodel.matchers.MlagMatchersImpl.HasPeerInterface;

/** Matchers for {@link Mlag} */
@ParametersAreNonnullByDefault
public final class MlagMatchers {

  /** Provides a matcher that matches if given {@link Mlag} has the specified id. */
  public static HasId hasId(String id) {
    return new HasId(id, equalTo(id));
  }

  /** Provides a matcher that matches if given {@link Mlag} has the specified peer address. */
  public static HasPeerAddress hasPeerAddress(Ip ip) {
    return new HasPeerAddress(ip, equalTo(ip));
  }

  /** Provides a matcher that matches if given {@link Mlag} has the specified peer interface. */
  public static HasPeerInterface hasPeerInterface(String name) {
    return new HasPeerInterface(name, equalTo(name));
  }

  /** Provides a matcher that matches if given {@link Mlag} has the specified local interface. */
  public static HasLocalInterface hasLocalInterface(String name) {
    return new HasLocalInterface(name, equalTo(name));
  }

  // Prevent initialization
  private MlagMatchers() {}
}

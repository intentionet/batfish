package org.batfish.symbolic;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.batfish.common.BatfishException;

/**
 * The OSPF type of a message. This is either intra area (O), inter area (OIA), external type 1
 * (E1), or external type 2 (E2)
 *
 * @author Ryan Beckett
 */
public enum OspfType {
  O,
  OIA,
  E1,
  E2;

  public static final List<OspfType> values = ImmutableList.of(O, OIA, E1, E2);

  @Override
  public String toString() {
    switch (this) {
      case O:
        return "O";
      case OIA:
        return "O IA";
      case E1:
        return "O E1";
      case E2:
        return "O E2";
      default:
        throw new BatfishException("Invalid Ospf Type");
    }
  }
}

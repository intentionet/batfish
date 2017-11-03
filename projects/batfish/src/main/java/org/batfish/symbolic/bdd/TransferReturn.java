package org.batfish.symbolic.bdd;

import net.sf.javabdd.BDD;
import org.batfish.symbolic.utils.Tuple;

public class TransferReturn extends Tuple<BDDRoute, BDD> {

  TransferReturn(BDDRoute r, BDD b) {
    super(r, b);
  }

  public String debug() {
    return this.getFirst().dot(this.getSecond());
  }
}

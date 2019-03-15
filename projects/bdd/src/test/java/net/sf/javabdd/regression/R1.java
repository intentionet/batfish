// R1.java, created Jul 28, 2004 2:22:19 AM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.javabdd.regression;

import junit.framework.Assert;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.bdd.BDDTestCase;

/**
 * satCount bug
 *
 * @author John Whaley
 * @version $Id: R1.java,v 1.6 2004/10/19 11:46:30 joewhaley Exp $
 */
public class R1 extends BDDTestCase {
  public static void main(String[] args) {
    junit.textui.TestRunner.run(R1.class);
  }

  public void testR1() {
    Assert.assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = nextFactory();
      BDDDomain d = bdd.extDomain(new int[] {16})[0];
      BDD x = d.ithVar(6).orWith(d.ithVar(13));
      BDD set = d.set();
      double s1 = x.satCount(set);
      if (bdd.varNum() < 20) {
        bdd.setVarNum(20);
      }
      double s2 = x.satCount(set);
      Assert.assertEquals(bdd.toString(), s1, s2, 0.00001);
      x.free();
      set.free();
    }
  }
}

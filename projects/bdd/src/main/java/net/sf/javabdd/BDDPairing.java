// BDDPairing.java, created Jan 29, 2003 9:50:57 PM by jwhaley
// Copyright (C) 2003 John Whaley
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.javabdd;

/**
 * Encodes a table of variable pairs. This is used for replacing variables in a BDD.
 *
 * @author John Whaley
 * @version $Id: BDDPairing.java,v 1.1 2004/10/16 02:58:57 joewhaley Exp $
 */
public abstract class BDDPairing {

  /**
   * Adds the pair (oldvar, newvar) to this table of pairs. This results in oldvar being substituted
   * with newvar in a call to BDD.replace().
   *
   * <p>Compare to bdd_setpair.
   */
  public abstract void set(int oldvar, int newvar);

  /**
   * Like set(), but with a whole list of pairs.
   *
   * <p>Compare to bdd_setpairs.
   */
  public void set(int[] oldvar, int[] newvar) {
    if (oldvar.length != newvar.length) {
      throw new BDDException();
    }

    for (int n = 0; n < oldvar.length; n++) {
      this.set(oldvar[n], newvar[n]);
    }
  }

  /**
   * Adds the pair (oldvar, newvar) to this table of pairs. This results in oldvar being substituted
   * with newvar in a call to bdd.replace(). The variable oldvar is substituted with the BDD newvar.
   * The possibility to substitute with any BDD as newvar is utilized in BDD.compose(), whereas only
   * the topmost variable in the BDD is used in BDD.replace().
   *
   * <p>Compare to bdd_setbddpair.
   */
  public abstract void set(int oldvar, BDD newvar);

  /**
   * Like set(), but with a whole list of pairs.
   *
   * <p>Compare to bdd_setbddpairs.
   */
  public void set(int[] oldvar, BDD[] newvar) {
    if (oldvar.length != newvar.length) {
      throw new BDDException();
    }

    for (int n = 0; n < newvar.length; n++) {
      this.set(oldvar[n], newvar[n]);
    }
  }

  /**
   * Defines each variable in the finite domain block p1 to be paired with the corresponding
   * variable in p2.
   *
   * <p>Compare to fdd_setpair.
   */
  public void set(BDDDomain p1, BDDDomain p2) {
    int[] ivar1 = p1.vars();
    int[] ivar2 = p2.vars();
    this.set(ivar1, ivar2);
  }

  /**
   * Like set(), but with a whole list of pairs.
   *
   * <p>Compare to fdd_setpairs.
   */
  public void set(BDDDomain[] p1, BDDDomain[] p2) {
    if (p1.length != p2.length) {
      throw new BDDException();
    }

    for (int n = 0; n < p1.length; n++) {
      if (p1[n].varNum() != p2[n].varNum()) {
        throw new BDDException();
      }
    }

    for (int n = 0; n < p1.length; n++) {
      this.set(p1[n], p2[n]);
    }
  }

  /**
   * Resets this table of pairs by setting all substitutions to their default values (that is, no
   * change).
   *
   * <p>Compare to bdd_resetpair.
   */
  public abstract void reset();
}

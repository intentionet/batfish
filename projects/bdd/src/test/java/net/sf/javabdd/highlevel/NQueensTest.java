// NQueensTest.java, created Jul 28, 2004 1:44:28 AM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.javabdd.highlevel;

import junit.framework.Assert;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.bdd.BDDTestCase;

/**
 * NQueensTest
 *
 * @author John Whaley
 * @version $Id: NQueensTest.java,v 1.1 2004/10/19 11:46:29 joewhaley Exp $
 */
public class NQueensTest extends BDDTestCase {
  public static void main(String[] args) {
    junit.textui.TestRunner.run(NQueensTest.class);
  }

  public static final int CHECK = 10;
  public static final double[] ANSWERS = {
    1.,
    0.,
    0.,
    2.,
    10.,
    4.,
    40.,
    92.,
    352.,
    724.,
    2680.,
    14200.,
    73712.,
    365596.,
    2279184.,
    14772512.,
    95815104.,
    666090624.,
    4968057848.,
    39029188884.,
    314666222712.,
    2691008701644.,
    24233937684440.
  };

  public void testNQueens() {
    Assert.assertTrue(hasNext());
    int numberOfNodes = (int) (Math.pow(4.42, CHECK - 6)) * 1000;
    while (hasNext()) {
      NQueens.B = nextFactory();
      NQueens.B.setNodeTableSize(numberOfNodes);
      for (int i = 1; i <= CHECK; ++i) {
        NQueens.N = i;
        double n = NQueens.runTest();
        Assert.assertEquals(n, ANSWERS[i - 1], 0.1);
        NQueens.freeAll();
      }
    }
  }

  private static class NQueens {

    private static BDDFactory B;
    private static int N; /* Size of the chess board */
    private static BDD[][] X; /* BDD variable array */
    private static BDD queen; /* N-queen problem expressed as a BDD */
    private static BDD solution; /* One solution */

    private static double runTest() {

      if (B.varNum() < N * N) {
        B.setVarNum(N * N);
      }

      queen = B.one();

      int i, j;

      /* Build variable array */
      X = new BDD[N][N];
      for (i = 0; i < N; i++) {
        for (j = 0; j < N; j++) {
          X[i][j] = B.ithVar(i * N + j);
        }
      }

      /* Place a queen in each row */
      for (i = 0; i < N; i++) {
        BDD e = B.zero();
        for (j = 0; j < N; j++) {
          e.orWith(X[i][j].id());
        }
        queen.andWith(e);
      }

      /* Build requirements for each variable(field) */
      for (i = 0; i < N; i++) {
        for (j = 0; j < N; j++) {
          build(i, j);
        }
      }

      solution = queen.satOne();

      double result = queen.satCount();
      return result;
    }

    private static void freeAll() {
      for (int i = 0; i < N; i++) {
        for (int j = 0; j < N; j++) {
          X[i][j].free();
        }
      }
      queen.free();
      solution.free();
    }

    private static void build(int i, int j) {
      BDD a = B.one(), b = B.one(), c = B.one(), d = B.one();
      int k, l;

      /* No one in the same column */
      for (l = 0; l < N; l++) {
        if (l != j) {
          BDD u = X[i][l].apply(X[i][j], BDDFactory.nand);
          a.andWith(u);
        }
      }

      /* No one in the same row */
      for (k = 0; k < N; k++) {
        if (k != i) {
          BDD u = X[i][j].apply(X[k][j], BDDFactory.nand);
          b.andWith(u);
        }
      }

      /* No one in the same up-right diagonal */
      for (k = 0; k < N; k++) {
        int ll = k - i + j;
        if (ll >= 0 && ll < N) {
          if (k != i) {
            BDD u = X[i][j].apply(X[k][ll], BDDFactory.nand);
            c.andWith(u);
          }
        }
      }

      /* No one in the same down-right diagonal */
      for (k = 0; k < N; k++) {
        int ll = i + j - k;
        if (ll >= 0 && ll < N) {
          if (k != i) {
            BDD u = X[i][j].apply(X[k][ll], BDDFactory.nand);
            d.andWith(u);
          }
        }
      }

      c.andWith(d);
      b.andWith(c);
      a.andWith(b);
      queen.andWith(a);
    }
  }
}

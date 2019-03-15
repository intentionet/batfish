// FindBestOrder.java, created Apr 2, 2004 10:43:21 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.javabdd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.StringTokenizer;

/**
 * FindBestOrder
 *
 * @author jwhaley
 * @version $Id: FindBestOrder.java,v 1.2 2005/10/13 05:59:47 joewhaley Exp $
 */
public class FindBestOrder {

  static BDDFactory bdd = null;
  boolean newbdd = true;
  BDD b1 = null;
  BDD b2 = null;
  BDD b3 = null;

  String filename0 = "fbo.bi";
  String filename1 = "fbo.1";
  String filename2 = "fbo.2";
  String filename3 = "fbo.3";

  /** How long to delay for loading, in ms. */
  long DELAY_TIME = Long.parseLong(System.getProperty("fbo.delaytime", "30000"));

  /** Factor how long to wait beyond the best time. */
  float FACTOR = Float.parseFloat(System.getProperty("fbo.waitfactor", "1.1"));

  BDDFactory.BDDOp op;
  long bestCalcTime;
  long bestTotalTime;
  String bestOrder;

  int nodeTableSize;
  int cacheSize;
  int maxIncrease;

  File f0, f1, f2, f3;

  public FindBestOrder(
      int nodeTableSize, int cacheSize, int maxIncrease, long bestTime, long delayTime) {
    this.bestCalcTime = bestTime;
    this.bestTotalTime = Long.MAX_VALUE;
    // this.nodeTableSize = b1.getFactory().getAllocNum();
    this.nodeTableSize = nodeTableSize;
    this.cacheSize = cacheSize;
    this.maxIncrease = maxIncrease;
    this.DELAY_TIME = delayTime;
  }

  public void init(BDD b1, BDD b2, BDD dom, BDDFactory.BDDOp op) throws IOException {
    this.op = op;
    f0 = File.createTempFile("fbo", "a");
    filename0 = f0.getAbsolutePath();
    f0.deleteOnExit();
    f1 = File.createTempFile("fbo", "b");
    filename1 = f1.getAbsolutePath();
    f1.deleteOnExit();
    f2 = File.createTempFile("fbo", "c");
    filename2 = f2.getAbsolutePath();
    f2.deleteOnExit();
    f3 = File.createTempFile("fbo", "d");
    filename3 = f3.getAbsolutePath();
    f3.deleteOnExit();
    // System.out.print("Writing BDDs to files...");
    writeBDDConfig(b1.getFactory(), filename0);
    b1.getFactory().save(filename1, b1);
    b2.getFactory().save(filename2, b2);
    dom.getFactory().save(filename3, dom);
    // System.out.println("done.");
  }

  public void cleanup() {
    // System.out.println("Cleaning up temporary files.");
    f0.delete();
    f1.delete();
    f2.delete();
    f3.delete();
    if (b1 != null) {
      b1.free();
    }
    if (b2 != null) {
      b2.free();
    }
    if (b3 != null) {
      b3.free();
    }
  }

  public void writeBDDConfig(BDDFactory bdd, String fileName) throws IOException {
    BufferedWriter dos = null;
    try {
      dos = new BufferedWriter(new FileWriter(fileName));
      for (int i = 0; i < bdd.numberOfDomains(); ++i) {
        BDDDomain d = bdd.getDomain(i);
        dos.write(d.getName() + " " + d.size() + "\n");
      }
    } finally {
      if (dos != null) {
        dos.close();
      }
    }
  }

  public long tryOrder(boolean reverse, String varOrder) {
    System.gc();
    TryThread t = new TryThread();
    t.reverse = reverse;
    t.varOrderToTry = varOrder;
    t.start();
    try {
      long waitTime = (long) (bestTotalTime * FACTOR) + DELAY_TIME;
      if (waitTime < 0L) {
        waitTime = Long.MAX_VALUE;
      }
      t.join(waitTime);
    } catch (InterruptedException x) {
    }
    t.stop();
    Thread.yield(); // Help ThreadDeath exception to propagate.
    if (t.totalTime == Long.MAX_VALUE) {
      System.out.println("Thread taking too long, aborted.");
      System.out.print("Free memory: " + Runtime.getRuntime().freeMemory());
      b1 = null;
      b2 = null;
      b3 = null;
      bdd = null;
      newbdd = true;
      System.gc();
      System.out.println(" bytes -> " + Runtime.getRuntime().freeMemory() + " bytes");
    }
    if (t.time < bestCalcTime) {
      bestOrder = varOrder;
      bestCalcTime = t.time;
      if (t.totalTime < bestTotalTime) {
        bestTotalTime = t.totalTime;
      }
    }
    return t.time;
  }

  public String getBestOrder() {
    return bestOrder;
  }

  public long getBestTime() {
    return bestCalcTime;
  }

  public class TryThread extends Thread {
    boolean reverse;
    String varOrderToTry;
    long time = Long.MAX_VALUE;
    long totalTime = Long.MAX_VALUE;

    @Override
    public void run() {
      long total = System.currentTimeMillis();
      if (bdd == null) {
        bdd = JFactory.init(nodeTableSize, cacheSize);
        bdd.setMaxIncrease(maxIncrease);
        readBDDConfig(bdd);
      }
      int[] varorder = bdd.makeVarOrdering(reverse, varOrderToTry);
      bdd.setVarOrder(varorder);
      // System.out.println("\nTrying ordering "+varOrderToTry);
      try {
        if (newbdd) {
          b1 = bdd.load(filename1);
          b2 = bdd.load(filename2);
          b3 = bdd.load(filename3);
          newbdd = false;
        }
        long t = System.currentTimeMillis();
        BDD result = b1.applyEx(b2, op, b3);
        time = System.currentTimeMillis() - t;
        // b1.free(); b2.free(); b3.free();
        result.free();
      } catch (IOException x) {
      }
      System.out.println("Ordering: " + varOrderToTry + " time: " + time);
      // bdd.done();
      totalTime = System.currentTimeMillis() - total;
    }

    public void readBDDConfig(BDDFactory bdd) {
      BufferedReader in = null;
      try {
        in = new BufferedReader(new FileReader(filename0));
        for (; ; ) {
          String s = in.readLine();
          if (s == null || s.equals("")) {
            break;
          }
          StringTokenizer st = new StringTokenizer(s);
          String name = st.nextToken();
          long size = Long.parseLong(st.nextToken()) - 1;
          makeDomain(bdd, name, BigInteger.valueOf(size).bitLength());
        }
      } catch (IOException x) {
      } finally {
        if (in != null) {
          try {
            in.close();
          } catch (IOException e) {
          }
        }
      }
    }

    BDDDomain makeDomain(BDDFactory bdd, String name, int bits) {
      BDDDomain d = bdd.extDomain(new long[] {1L << bits})[0];
      d.setName(name);
      return d;
    }
  }
}

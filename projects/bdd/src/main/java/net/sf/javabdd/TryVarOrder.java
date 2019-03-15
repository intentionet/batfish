// TryVarOrder.java, created Apr 2, 2004 10:43:21 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.javabdd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.StringTokenizer;

/**
 * TryVarOrder
 *
 * @author jwhaley
 * @version $Id: TryVarOrder.java,v 1.3 2005/10/13 05:37:07 joewhaley Exp $
 */
public class TryVarOrder {

  /** BDD Factory, reused if possible. */
  static Object bdd = null;

  static ClassLoader makeClassLoader() {
    // return HijackingClassLoader.makeClassLoader();
    return ClassLoader.getSystemClassLoader();
  }

  /**
   * Initialize the named BDD factory under a new class loader.
   *
   * @param s BDD factory to initialize
   */
  void initBDDFactory(String s) {
    try {
      ClassLoader cl;
      if (bddoperation != null) {
        cl = bddoperation.getClass().getClassLoader();
      } else {
        cl = makeClassLoader();
      }
      Class c = cl.loadClass("net.sf.javabdd.BDDFactory");
      Method m = c.getMethod("init", new Class[] {String.class, int.class, int.class});
      bdd = m.invoke(null, new Object[] {s, new Integer(nodeTableSize), new Integer(cacheSize)});
      m = c.getMethod("setMaxIncrease", new Class[] {int.class});
      m.invoke(bdd, new Object[] {new Integer(maxIncrease)});

      BufferedReader in = null;
      try {
        in = new BufferedReader(new FileReader(filename0));
        for (; ; ) {
          String s2 = in.readLine();
          if (s2 == null || s2.equals("")) {
            break;
          }
          StringTokenizer st = new StringTokenizer(s2);
          String name = st.nextToken();
          long size = Long.parseLong(st.nextToken()) - 1;
          makeDomain(c, name, BigInteger.valueOf(size).bitLength());
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
    } catch (Exception x) {
      System.err.println(
          "Exception occurred while initializing BDD factory: " + x.getLocalizedMessage());
      x.printStackTrace();
    }
  }

  /** Destroy the BDD factory. */
  void destroyBDDFactory() {
    if (bdd != null) {
      Class c = bdd.getClass();
      try {
        Method m = c.getMethod("done", new Class[] {});
        m.invoke(bdd, new Object[] {});
      } catch (Exception x) {
        System.err.println(
            "Exception occurred while destroying BDD factory: " + x.getLocalizedMessage());
        x.printStackTrace();
      }
      bdd = null;
    }
  }

  void setBDDError(int code) {
    Class c = bdd.getClass();
    try {
      Method m = c.getMethod("setError", new Class[] {int.class});
      m.invoke(bdd, new Object[] {new Integer(code)});
    } catch (Exception x) {
      System.err.println(
          "Exception occurred while setting error for BDD factory: " + x.getLocalizedMessage());
      x.printStackTrace();
    }
  }

  /**
   * Make a domain in the BDD factory.
   *
   * @param c BDD factory class
   * @param name name of domain
   * @param bits bits in domain
   * @throws Exception
   */
  static void makeDomain(Class c, String name, int bits) throws Exception {
    Method m = c.getMethod("extDomain", new Class[] {long[].class});
    Object[] ds = (Object[]) m.invoke(null, new Object[] {new long[] {1L << bits}});
    c = c.getClassLoader().loadClass("net.sf.javabdd.BDDDomain");
    m = c.getMethod("setName", new Class[] {String.class});
    m.invoke(ds[0], new Object[] {name});
  }

  Object bddoperation = null;

  void initBDDOperation() throws Exception {
    ClassLoader cl;
    if (bdd != null) {
      cl = bdd.getClass().getClassLoader();
    } else {
      cl = makeClassLoader();
    }
    Class bddop_class = cl.loadClass("net.sf.javabdd.TryVarOrder$BDDOperation");
    Constructor c = bddop_class.getConstructor(new Class[0]);
    bddoperation = c.newInstance();
    Method m = bddop_class.getMethod("setOp", new Class[] {int.class});
    m.invoke(bddoperation, new Object[] {new Integer(op.id)});
    m =
        bddop_class.getMethod(
            "setFilenames", new Class[] {String.class, String.class, String.class});
    m.invoke(bddoperation, new Object[] {filename1, filename2, filename3});
  }

  void setVarOrder(boolean reverse, String varOrderToTry) throws Exception {
    Class bddop_class = bddoperation.getClass();
    Method m = bddop_class.getMethod("setVarOrder", new Class[] {boolean.class, String.class});
    m.invoke(bddoperation, new Object[] {Boolean.valueOf(reverse), varOrderToTry});
  }

  void load() throws Exception {
    Class bddop_class = bddoperation.getClass();
    Method m = bddop_class.getMethod("load", new Class[] {});
    m.invoke(bddoperation, new Object[] {});
  }

  long doIt() throws Exception {
    Class bddop_class = bddoperation.getClass();
    Method m = bddop_class.getMethod("doIt", new Class[] {});
    Long result = (Long) m.invoke(bddoperation, new Object[] {});
    return result.longValue();
  }

  void free() throws Exception {
    Class bddop_class = bddoperation.getClass();
    Method m = bddop_class.getMethod("free", new Class[] {});
    m.invoke(bddoperation, new Object[] {});
  }

  public static class BDDOperation {

    public BDDOperation() {}

    public BDDOperation(int op, String f1, String f2, String f3) {
      setOp(op);
      setFilenames(f1, f2, f3);
    }

    public void setOp(int op) {
      switch (op) {
        case 0:
          this.op = BDDFactory.and;
          break;
        case 1:
          this.op = BDDFactory.xor;
          break;
        case 2:
          this.op = BDDFactory.or;
          break;
        case 3:
          this.op = BDDFactory.nand;
          break;
        case 4:
          this.op = BDDFactory.nor;
          break;
        case 5:
          this.op = BDDFactory.imp;
          break;
        case 6:
          this.op = BDDFactory.biimp;
          break;
        case 7:
          this.op = BDDFactory.diff;
          break;
        case 8:
          this.op = BDDFactory.less;
          break;
        case 9:
          this.op = BDDFactory.invimp;
          break;
        default:
          throw new InternalError("Invalid op: " + op);
      }
    }

    public void setFilenames(String f1, String f2, String f3) {
      this.filename1 = f1;
      this.filename2 = f2;
      this.filename3 = f3;
    }

    /** Operation for applyEx. */
    BDDFactory.BDDOp op;

    /** Filenames for inputs. */
    String filename1;

    String filename2;
    String filename3;

    /** Inputs to applyEx. */
    BDD b1 = null;

    BDD b2 = null;
    BDD b3 = null;

    public void setVarOrder(boolean reverse, String varOrderToTry) {
      BDDFactory f = (BDDFactory) bdd;
      int[] varorder = f.makeVarOrdering(reverse, varOrderToTry);
      f.setVarOrder(varorder);
    }

    public void load() throws IOException {
      BDDFactory f = (BDDFactory) bdd;
      if (b1 == null) {
        b1 = f.load(filename1);
        b2 = f.load(filename2);
        b3 = f.load(filename3);
      }
    }

    public long doIt() {
      long t = System.currentTimeMillis();
      BDD result = b1.applyEx(b2, op, b3);
      long time = System.currentTimeMillis() - t;
      result.free();
      return time;
    }

    public void free() {
      b1.free();
      b1 = null;
      b2.free();
      b2 = null;
      b3.free();
      b3 = null;
    }
  }

  BDDFactory.BDDOp op;

  /** Filename for BDD config. */
  String filename0;
  /** Filenames for inputs. */
  String filename1;

  String filename2;
  String filename3;

  /** How long to delay for loading, in ms. */
  long DELAY_TIME = Long.parseLong(System.getProperty("fbo.delaytime", "20000"));

  /** Factor how long to wait beyond the best time. */
  float FACTOR = Float.parseFloat(System.getProperty("fbo.waitfactor", "1.1"));

  /** Best calc time so far. */
  long bestCalcTime;
  /** Best varorder so far. */
  String bestOrder;

  /** Initial node table size. */
  int nodeTableSize;
  /** Initial cache size. */
  int cacheSize;
  /** Initial max increase. */
  int maxIncrease;

  /** File pointers for output and input BDDs. */
  File f0, f1, f2, f3;

  /** Construct a new TryVarOrder. */
  public TryVarOrder(
      int nodeTableSize, int cacheSize, int maxIncrease, long bestTime, long delayTime) {
    this.bestCalcTime = bestTime;
    // this.nodeTableSize = b1.getFactory().getAllocNum();
    this.nodeTableSize = nodeTableSize;
    this.cacheSize = cacheSize;
    this.maxIncrease = maxIncrease;
    this.DELAY_TIME = delayTime;
  }

  /**
   * Initialize for a new trial. Takes the given input BDDs and saves them out to temporary files.
   *
   * @param b1 first input to applyEx
   * @param b2 second input to applyEx
   * @param dom third input to applyEx
   * @param op operation to be passed to applyEx
   * @throws IOException
   */
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
    try {
      initBDDOperation();
    } catch (Exception x) {
      System.err.println("Exception occurred while initializing: " + x.getLocalizedMessage());
      x.printStackTrace();
    }
  }

  /** Clean up the temporary files. */
  public void cleanup() {
    // System.out.println("Cleaning up temporary files.");
    try {
      f0.delete();
      f1.delete();
      f2.delete();
      f3.delete();
      free();
    } catch (Exception x) {
      System.err.println("Exception occurred while cleaning up: " + x.getLocalizedMessage());
      x.printStackTrace();
    }
  }

  /**
   * Write the BDD configuration to a file.
   *
   * @param bdd BDD factory
   * @param fileName filename
   * @throws IOException
   */
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

  /**
   * Try out a variable order.
   *
   * @param reverse whether to reverse the bits
   * @param varOrder variable order to try
   * @return time spent, or Long.MAX_VALUE if it didn't terminate
   */
  public long tryOrder(String factory, boolean reverse, String varOrder) {
    if (bdd == null) {
      initBDDFactory(factory);
    }
    // System.gc();
    TryThread t = new TryThread(reverse, varOrder);
    t.start();
    try {
      t.join(DELAY_TIME);
      if (t.initTime >= 0L) {
        t.join((long) (bestCalcTime * FACTOR));
      }
    } catch (InterruptedException x) {
    }
    if (t.isAlive()) {
      setBDDError(1);
      try {
        t.join();
      } catch (InterruptedException x) {
      }
      System.out.print("Free memory: " + Runtime.getRuntime().freeMemory());
      destroyBDDFactory();
      System.gc();
      System.out.println(" bytes -> " + Runtime.getRuntime().freeMemory() + " bytes");
    }
    if (t.computeTime < 0) {
      if (t.initTime < 0) {
        // Couldn't even initialize in time!
        System.out.println("BDD factory took too long to initialize, aborted.");
      } else {
        System.out.println("Took too long to compute, aborted.");
      }
    } else if (t.computeTime < bestCalcTime) {
      bestOrder = varOrder;
      bestCalcTime = t.computeTime;
    }
    return t.computeTime;
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
    long initTime = -1;
    long computeTime = -1;

    TryThread(boolean r, String v) {
      reverse = r;
      varOrderToTry = v;
    }

    @Override
    public void run() {
      // System.out.println("\nTrying ordering "+varOrderToTry);
      try {
        long time = System.currentTimeMillis();
        setVarOrder(reverse, varOrderToTry);
        load();
        initTime = time - System.currentTimeMillis();
        computeTime = doIt();
        free();
        System.out.println(
            "Ordering: " + varOrderToTry + " init: " + initTime + " compute: " + computeTime);
      } catch (Exception x) {
        System.err.println("Exception occurred while trying order: " + x.getLocalizedMessage());
        x.printStackTrace();
      }
    }
  }
}

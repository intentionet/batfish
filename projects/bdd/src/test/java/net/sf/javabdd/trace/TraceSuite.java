// TraceSuite.java, created Nov 17, 2004 10:07:46 AM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.javabdd.trace;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 *
 * <pre>
 * This class opens and runs a suite of BDD traces from a zip file.
 * A net.sf.javabdd.trace file is assumed to have the '.net.sf.javabdd.trace' suffix. Any file with the 'README' posfix
 * is assumed to be an information file and is dumped to stdout.
 * </pre>
 *
 * Based on the version in JDD.
 *
 * @version $Id: TraceSuite.java,v 1.1 2004/11/17 18:38:38 joewhaley Exp $
 */
public class TraceSuite {

  /**
   * filename is the name of zip file or directory, initial_size is the suggested "base" for
   * deciding the initial of the nodetable. if initial_size is -1, it is ignored and TraceDriver's
   * default value is used
   */
  public TraceSuite(String filename, int initial_size) {
    File dir = new File(filename);
    if (dir.isDirectory()) {
      runDirectory(dir, initial_size);
    } else {
      runZip(filename, initial_size);
    }
  }

  public void runDirectory(File dir, int initial_size) {
    File[] files = dir.listFiles();
    if (files == null) {
      return;
    }
    for (int i = 0; i < files.length; ++i) {
      if (files[i].isDirectory()) {
        runDirectory(files[i], initial_size);
      } else {
        try {
          String name = files[i].getName();
          InputStream is = new BufferedInputStream(new FileInputStream(files[i]));
          if (name.endsWith(".net.sf.javabdd.trace")) {
            runTrace(name, is, initial_size);
          } else if (name.endsWith("README")) {
            showFile(name, is);
          }
        } catch (IOException exx) {
          System.out.println("FAILED: " + exx.getMessage() + "\n");
          exx.printStackTrace();
          System.exit(20);
        }
      }
    }
  }

  public void runZip(String filename, int initial_size) {
    try {
      InputStream is = new FileInputStream(filename);
      ZipInputStream zis = new ZipInputStream(is);

      System.out.println("\n***** [ " + filename + " ] *****");
      // JREInfo.show();

      ZipEntry ze = zis.getNextEntry();
      while (zis.available() != 0) {

        String name = ze.getName();

        if (name.endsWith(".net.sf.javabdd.trace")) {
          runTrace(name, zis, initial_size);
        } else if (name.endsWith("README")) {
          showFile(name, zis);
        }

        zis.closeEntry();
        ze = zis.getNextEntry();
      }
      zis.close();
      is.close();
    } catch (IOException exx) {
      System.out.println("FAILED: " + exx.getMessage() + "\n");
      exx.printStackTrace();
      System.exit(20);
    }
  }

  private void runTrace(String name, InputStream is, int size) {
    // enable verbose temporary!
    boolean save = TraceDriver.verbose;
    TraceDriver.verbose = true;

    System.err.println("Tracing " + name + "...");
    try {
      if (size == -1) {
        new TraceDriver(name, is);
      } else {
        new TraceDriver(name, is, size);
      }
    } catch (Exception exx) {
      System.out.println("FAILED: " + exx.getMessage() + "\n\n");
      exx.printStackTrace();
    }

    TraceDriver.verbose = save; // set back verbose to its old value

    // let's cleanup, so we dont affect the next run so much:
    for (int i = 0; i < 6; i++) {
      System.gc();
    }

    try {
      Thread.sleep(5000);
    } catch (Exception ignored) {
    } // calm down!
  }

  private void showFile(String name, InputStream is) throws IOException {
    System.out.println("File " + name);
    byte[] buffer = new byte[10240];

    for (; ; ) {
      int i = is.read(buffer, 0, buffer.length);
      if (i <= 0) {
        return;
      }
      System.out.println(new String(buffer, 0, i));
    }
  }

  // -------------------------------------------------------------

  public static void main(String[] args) {
    if (args.length == 1) {
      new TraceSuite(args[0], -1);
    } else if (args.length == 2) {
      new TraceSuite(args[0], Integer.parseInt(args[1]));
    } else {
      System.err.println(
          "Usage: java "
              + TraceSuite.class.getName()
              + " <net.sf.javabdd.trace-suite.zip> [initial size _base_]");
    }
  }
}

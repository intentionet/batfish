// TraceDriver.java, created Nov 17, 2004 12:32:38 AM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.javabdd.trace;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.BDDPairing;

/**
 * A driver to run BDD net.sf.javabdd.trace files in the bwolen BDD net.sf.javabdd.trace format. See
 * http://www-2.cs.cmu.edu/~bwolen/software/.
 *
 * <p>This code is based on the code in JDD.
 *
 * @author jwhaley
 * @version $Id: TraceDriver.java,v 1.4 2005/05/09 10:28:46 joewhaley Exp $
 */
public class TraceDriver {

  static PrintStream out = System.out;

  // --------------------------------------------------------------
  class TracedVariable {
    public String name;
    public int index, last_use;
    BDD bdd;
    public boolean is_var = false;

    public void showName() {
      out.print(name);
    }

    public void show() {
      out.print("\n\t");
      showName();
      out.println();
      bdd.printSet();
    }
  }

  // --------------------------------------------------------------
  abstract class TracedOperation {
    public int index, size = -1;
    public String op;

    public void show() {}

    public abstract void execute() throws IOException;

    public void show_code() {}
  }

  // --------------------------------------------------------------
  class TracedDebugOperation extends TracedOperation {
    public String text;

    @Override
    public void execute() {
      if (verbose /*|| true*/) {
        out.println(text);
      }
    }

    @Override
    public void show_code() {
      out.println("//" + text);
    }
  }

  class TracedSaveOperation extends TracedOperation {
    public TracedVariable v;

    @Override
    public void execute() {
      try {
        bdd.save(v.name + ".buddy", v.bdd);
        // BDDIO.save(bdd, v.bdd, v.name + ".bdd");
      } catch (IOException exx) {
        // ignore
      }
    }

    @Override
    public void show_code() {
      out.println("BDDIO.saveBuDDy(bdd, " + v.bdd + ",\"" + v.name + ".buddy\");");
    }
  }

  class TracedPrintOperation extends TracedOperation {
    public TracedVariable v;
    public boolean graph;

    @Override
    public void execute() {
      if (graph) {
        v.bdd.printDot(); // bdd.printDot(v.name, v.bdd);
      } else {
        out.println(v.name + ":");
        v.bdd.printSet();
      }
    }

    @Override
    public void show_code() {
      if (graph) {
        out.println(v.name + ".printDot();");
      } else {
        out.println(v.name + ".printSet();");
      }
    }
  }

  class TracedCheckOperation extends TracedOperation {
    public TracedVariable t1, t2;

    @Override
    public void execute() throws IOException {
      boolean equal = (t1.bdd.equals(t2.bdd));
      if (size != -1) {
        boolean expected = (size == 0 ? false : true);
        if (equal != expected) {
          throw new IOException(
              "are_equal("
                  + t1.name
                  + ", "
                  + t2.name
                  + ") failed. expected "
                  + expected
                  + ", got "
                  + equal);
        }
      }
    }

    @Override
    public void show() {
      out.print(index + "\t");
      out.print("are_equal(" + t1.name + ", " + t2.name + ");");
      if (size != -1) {
        out.print("\t% " + size);
      }
      out.println();
    }
  }

  class TracedReorderOperation extends TracedOperation {
    public BDDFactory.ReorderMethod method;

    @Override
    public void execute() throws IOException {
      bdd.reorder(method);
    }

    @Override
    public void show() {
      out.print(index + "\t");
      out.print("reorder(" + method + ");");
      out.println();
    }
  }

  // --------------------------------------------------------------
  class TracedBDDOperation extends TracedOperation {
    public int ops;
    public TracedVariable ret, op1, op2, op3;
    public Vector operands;

    @Override
    public void show() {
      out.print(index + "\t");
      ret.showName();
      out.print(" = ");

      if (op.equals("=")) {
        op1.showName();
        out.print(";");
      } else {
        out.print(op + "(");
        boolean first = true;
        for (Enumeration e = operands.elements(); e.hasMoreElements(); ) {
          TracedVariable v = (TracedVariable) e.nextElement();
          if (first) {
            first = false;
          } else {
            out.print(", ");
          }
          v.showName();
        }
        out.print(");");
      }

      if (size != -1) {
        out.print("\t% " + size);
      }
      out.println();
    }

    @Override
    public void execute() throws IOException {
      // check_all_variables(); // DEBUG

      ret.bdd.free();

      if (op.equals("not")) {
        do_not();
      } else if (op.equals("=")) {
        do_assign();
      } else if (op.equals("and")) {
        do_and();
      } else if (op.equals("or")) {
        do_or();
      } else if (op.equals("xor")) {
        do_xor();
      } else if (op.equals("xnor")) {
        do_xnor();
      } else if (op.equals("nor")) {
        do_nor();
      } else if (op.equals("nand")) {
        do_nand();
      } else if (op.equals("ite")) {
        do_ite();
      } else if (op.equals("vars_curr_to_next")) {
        do_s2sp();
      } else if (op.equals("vars_next_to_curr")) {
        do_sp2s();
      } else if (op.equals("support_vars")) {
        do_support();
      } else if (op.equals("exists")) {
        do_exists();
      } else if (op.equals("forall")) {
        do_forall();
      } else if (op.equals("restrict")) {
        do_restrict();
      } else if (op.equals("rel_prod")) {
        do_relprod();
      } else {
        throw new IOException("Unknown operation '" + op + "', #" + op_count);
      }

      last_assignment = ret;

      if (size != -1) {
        int size2 = node_count(ret);
        if (size != size2) {
          out.println(
              "\n*************************************************************************");
          out.println(
              "Size comparison failed after " + op + " ( wanted " + size + ", got " + size2 + ")");
          show();
          out.println("\n");
          throw new IOException("Size comparison failed");
        }
      }

      checkVar(this);
    }

    // -------------------------------------------------------------------
    private void do_not() throws IOException {
      checkEquality(ops, 1, "do_not");
      ret.bdd = op1.bdd.not();
    }

    private void do_assign() throws IOException {
      checkEquality(ops, 1, "do_assign");
      ret.bdd = op1.bdd;
    }

    private void do_or() {
      if (ops == 2) {
        ret.bdd = op1.bdd.or(op2.bdd);
      } else {
        for (Enumeration e = operands.elements(); e.hasMoreElements(); ) {
          if (((TracedVariable) e.nextElement()).bdd.isOne()) {
            ret.bdd = bdd.one();
            return;
          }
        }

        BDD tmp = bdd.zero();
        for (Enumeration e = operands.elements(); e.hasMoreElements(); ) {
          TracedVariable v = (TracedVariable) e.nextElement();
          BDD tmp2 = tmp.or(v.bdd);
          tmp.free();
          tmp = tmp2;
        }
        ret.bdd = tmp;
      }
    }

    private void do_and() {
      if (ops == 2) {
        ret.bdd = op1.bdd.and(op2.bdd);
      } else {
        for (Enumeration e = operands.elements(); e.hasMoreElements(); ) {
          if (((TracedVariable) e.nextElement()).bdd.isZero()) {
            ret.bdd = bdd.zero();
            return;
          }
        }

        BDD tmp = bdd.one();
        for (Enumeration e = operands.elements(); e.hasMoreElements(); ) {
          TracedVariable v = (TracedVariable) e.nextElement();
          BDD tmp2 = tmp.and(v.bdd);
          tmp.free();
          tmp = tmp2;
        }
        ret.bdd = tmp;
      }
    }

    private void do_nand() {
      if (ops == 2) {
        ret.bdd = op1.bdd.apply(op2.bdd, BDDFactory.nand);
      } else {
        do_and();
        BDD tmp = ret.bdd;
        ret.bdd = tmp.not();
        tmp.free();
      }
    }

    private void do_nor() {
      if (ops == 2) {
        ret.bdd = op1.bdd.apply(op2.bdd, BDDFactory.nor);
      } else {
        do_or();
        BDD tmp = ret.bdd;
        ret.bdd = tmp.not();
        tmp.free();
      }
    }

    private void do_xor() throws IOException {
      check(ops == 2);
      ret.bdd = op1.bdd.xor(op2.bdd);
    }

    private void do_xnor() throws IOException {
      check(ops == 2);
      ret.bdd = op1.bdd.biimp(op2.bdd);
    }

    private void do_ite() throws IOException {
      check(ops == 3);
      ret.bdd = op1.bdd.ite(op2.bdd, op3.bdd);
    }

    private void do_s2sp() throws IOException {
      check(ops == 1);
      ret.bdd = op1.bdd.replace(s2sp);
    }

    private void do_sp2s() throws IOException {
      check(ops == 1);
      ret.bdd = op1.bdd.replace(sp2s);
    }

    private void do_support() throws IOException {
      check(ops == 1);
      ret.bdd = op1.bdd.support();
    }

    private void do_exists() throws IOException {
      check(ops == 2);
      ret.bdd = op2.bdd.exist(op1.bdd);
    }

    private void do_forall() throws IOException {
      check(ops == 2);
      ret.bdd = op2.bdd.forAll(op1.bdd);
    }

    private void do_restrict() throws IOException {
      check(ops == 2);
      ret.bdd = op1.bdd.restrict(op2.bdd);
    }

    private void do_relprod() throws IOException {
      check(ops == 3);
      ret.bdd = op2.bdd.relprod(op3.bdd, op1.bdd);
    }

    // -------------------------------------------------------------------------

    @Override
    public void show_code() {
      String code;
      Enumeration e = operands.elements();
      TracedVariable v = (TracedVariable) e.nextElement();
      if (op.equals("=")) {
        out.println("BDD " + ret.name + " = " + v.name + ";");
      } else {
        out.print("BDD " + ret.name + " = " + v.name + "." + op);
        out.print("(");
        boolean mode2 = op.equals("ite");
        int i = 0;
        for (i = 0; e.hasMoreElements(); i++) {
          v = (TracedVariable) e.nextElement();
          if (mode2 && i != 0) {
            out.print(", ");
          }
          out.print(v.name);

          if (e.hasMoreElements() && !mode2) {
            out.print("." + op + "(");
          }
        }
        if (!mode2) {
          for (int j = 1; j < i; j++) {
            out.print(")");
          }
        }

        out.println(");");
      }

      if (op.equals("ite")) {
        out.println("System.out.println(\"" + ret.name + " ==> \"+" + ret.name + ".nodeCount());");
      }
    }
  }

  // -----------------------------------------------
  private static final int DEFAULT_NODES = 500000, MAX_NODES = 3000000;
  private BDDFactory bdd;
  private InputStream is;
  private StringBuffer sb;
  private String filename, module;
  private int[] stack;
  private int stack_tos, nodes, cache, vars;
  private static int auto_reorder = Integer.parseInt(System.getProperty("reorder", "0"));
  private HashMap map;
  private BDDPairing s2sp, sp2s;
  private TracedVariable last_assignment;
  private Vector operations, variables;
  private int op_count, line_count, var_count;
  private long time;

  /** this is our extra VERBOSE flags, to enable trace_verbose_print() output */
  public static boolean verbose = false;

  public TraceDriver(String file) throws IOException {
    this(file, DEFAULT_NODES);
  }

  public TraceDriver(String file, int nodes) throws IOException {
    this(
        file,
        file.endsWith(".gz")
            ? (InputStream) new GZIPInputStream(new FileInputStream(file))
            : (InputStream) new FileInputStream(file),
        nodes);
  }

  public TraceDriver(String file, InputStream is) throws IOException {
    this(file, is, DEFAULT_NODES);
  }

  public TraceDriver(String file, InputStream is, int nodes) throws IOException {
    this.filename = file;
    this.nodes = nodes;
    this.is = is;
    this.sb = new StringBuffer();
    this.stack = new int[64];
    this.stack_tos = 0;
    this.cache = Math.max(Math.min(nodes / 10, 5000), 50000);
    this.map = new HashMap(1024);

    this.operations = new Vector();
    this.variables = new Vector();
    this.op_count = 0;
    this.line_count = 1;
    this.var_count = 0;

    // Options.verbose = true; // DEBUG

    TracedVariable vret = new TracedVariable();
    vret.last_use = 0;
    vret.name = "0";
    // vret.bdd = bdd.zero();
    map.put("0", vret);

    vret = new TracedVariable();
    vret.last_use = 0;
    vret.name = "1";
    // vret.bdd = bdd.one();
    map.put("1", vret);

    last_assignment = null;

    parse();
    // show_code();

    vret = (TracedVariable) map.get("0");
    vret.bdd = bdd.zero();
    vret = (TracedVariable) map.get("1");
    vret.bdd = bdd.one();

    execute();

    show_results();
    bdd.done();
  }

  // -----------------------------------------------------
  private void show_code() {
    out.println(
        "import org.sf.javabdd.*;\n"
            + "public class Test {\n"
            + "public static void main(String[] args) {\n");

    out.println(
        "\n\n"
            + "BDDFactory B = BDDFactory.init("
            + nodes
            + ",100);\n"
            + "B.setVarNum("
            + variables.size()
            + ");\nBDD ");

    int i = 0;
    for (Enumeration e = variables.elements(); e.hasMoreElements(); ) {
      TracedVariable v = (TracedVariable) e.nextElement();
      if (v.is_var) {
        if (i != 0) {
          out.print(",");
        }
        out.print(v.name + "=B.ithVar(" + i + ") ");
        i++;
      }
    }
    out.println(";");

    for (Enumeration e = operations.elements(); e.hasMoreElements(); ) {
      TracedOperation v = (TracedOperation) e.nextElement();
      v.show_code();
    }

    out.println("}\n}\n");
  }
  // -----------------------------------------------------
  private void setup_bdd(int vars) {
    this.vars = vars;
    // nodes = (int)Math.min( MAX_NODES, nodes * (1 + Math.log(1+vars)) );

    out.println();
    out.println(
        "loading " + module + " from " + filename + " (" + nodes + " nodes, " + vars + " vars)");

    bdd = BDDFactory.init(nodes, cache);
    if (auto_reorder != 0) {
      out.println("setting auto reorder to " + auto_reorder);
      bdd.autoReorder(getReorderMethod(auto_reorder));
      try {
        java.lang.reflect.Method cb =
            TraceDriver.class.getDeclaredMethod(
                "reorder_callback", new Class[] {boolean.class, BDDFactory.ReorderStats.class});
        bdd.registerReorderCallback(this, cb);
      } catch (NoSuchMethodException x) {
        System.out.println("Cannot find callback method");
      }
    }
    // bdd.setNodeNames(new TracedNames() );
  }

  public static void reorder_callback(boolean prestate, BDDFactory.ReorderStats s) {
    System.out.print(prestate ? "Start" : "Finish");
    System.out.println("ing reorder.");
    if (!prestate) {
      System.out.println(s);
    }
  }

  // -----------------------------------------------------
  private void alloc_var(String name) {
    TracedVariable vret = new TracedVariable();
    vret.last_use = 0;
    int vn = bdd.extVarNum(1);
    vret.bdd = bdd.ithVar(vn);
    vret.name = name;
    vret.is_var = true;
    map.put(name, vret);
    variables.add(vret);
    var_count++;
  }

  private void checkVar(TracedBDDOperation tp) {
    checkVar(tp.ret);
    for (Enumeration e = tp.operands.elements(); e.hasMoreElements(); ) {
      TracedVariable v = (TracedVariable) e.nextElement();
      checkVar(v);
    }
  }

  private void checkVar(TracedVariable v) {
    if (v != null && v.last_use == op_count) {
      // out.println("Removing " + v.name + " at state " + op_count + ", bdd = " + v.bdd);
      v.bdd.free();
      v.last_use = -1; // we dont want to remove this again, in case A = not(A) or B = and(A,A)
      // v.bdd = 0;
    }
  }

  private TracedPrintOperation createPrintOperation(boolean graph, TracedVariable v) {
    TracedPrintOperation tp = new TracedPrintOperation();
    tp.index = op_count;
    tp.graph = graph;
    tp.v = v;
    operations.add(tp);
    return tp;
  }

  private TracedSaveOperation createSaveOperation(TracedVariable v) {
    TracedSaveOperation ts = new TracedSaveOperation();
    ts.index = op_count;
    ts.v = v;
    operations.add(ts);
    return ts;
  }

  private TracedCheckOperation createCheckOperation(TracedVariable v1, TracedVariable v2) {
    TracedCheckOperation tp = new TracedCheckOperation();
    tp.index = op_count;
    tp.t1 = v1;
    tp.t2 = v2;
    operations.add(tp);
    return tp;
  }

  private TracedDebugOperation createDebugOperation(String text) {
    TracedDebugOperation tp = new TracedDebugOperation();
    tp.index = op_count;
    tp.text = text;
    operations.add(tp);
    return tp;
  }

  private TracedBDDOperation createBDDOperation() {
    TracedBDDOperation tp = new TracedBDDOperation();
    tp.index = op_count;
    operations.add(tp);
    tp.operands = new Vector(3);
    return tp;
  }

  private TracedReorderOperation createReorderOperation(BDDFactory.ReorderMethod m) {
    TracedReorderOperation tp = new TracedReorderOperation();
    tp.index = op_count;
    tp.method = m;
    operations.add(tp);
    return tp;
  }

  // -----------------------------------------------------

  private static final boolean TRACE_LAST_ASSIGNMENT = false;

  private void show_results() {
    time = System.currentTimeMillis() - time;
    out.println("" + op_count + " operations performed, total execution time: " + time + " [ms]");

    if (verbose) {
      if (TRACE_LAST_ASSIGNMENT && last_assignment != null && last_assignment.hashCode() != -1) {
        int size = node_count(last_assignment);
        out.println("Last assignment: " + last_assignment.name + ", " + size + " nodes.");
        // if(size < 20) bdd.printSet(last_assignment.bdd);
        out.println("\n");
      }
      out.println("Nodes: " + bdd.getNodeNum() + "/" + bdd.getNodeTableSize());
      out.println(bdd.getGCStats());
      bdd.printStat();
    }

    if (auto_reorder != 0) {
      out.println("Final variable order:");
      bdd.printOrder();
    }
  }

  /** check if the variables to be used are OK */
  private void check_all_variables() {
    for (Enumeration e = variables.elements(); e.hasMoreElements(); ) {
      TracedVariable v = (TracedVariable) e.nextElement();
      if (v.last_use >= op_count) {
        // v.showName();out.println();
        // bdd.check_node(v.bdd, v.name); // DEBUG
      }
    }
  }

  // -------------------------------------------------------------------------
  private void execute() throws IOException {
    time = System.currentTimeMillis();
    for (Enumeration e = operations.elements(); e.hasMoreElements(); ) {
      TracedOperation tp = (TracedOperation) e.nextElement();
      op_count = tp.index;

      if (TraceDriver.verbose) {
        tp.show(); // DEBUG !!
      }
      tp.execute();
    }
  }

  /** BDD net.sf.javabdd.trace driver doesn't count nodes the same way as we do ... */
  private int node_count(TracedVariable v) {
    if (v.bdd.hashCode() == -1) {
      throw new InternalError();
    }
    int size = v.bdd.nodeCount();
    // adjust BDD size to include terminals
    if (v.bdd.isOne() || v.bdd.isZero()) {
      size += 1;
    } else {
      size += 2;
    }
    return size;
  }

  private void parse() throws IOException {
    read_module();
    read_input();
    skip_output();
    read_structure();
  }

  private void read_module() throws IOException {
    need("MODULE");
    module = need();
  }

  private void skip_output() throws IOException {
    need("OUTPUT");
    for (String tmp = need(); !tmp.equals(";"); tmp = need()) {
    }
  }

  private void read_structure() throws IOException {
    need("STRUCTURE");
    while (true) {
      String ret = need();
      if (ret.equals("ENDMODULE")) {
        return;
      }

      op_count++;

      if (ret.equals("trace_verbose_print")) {
        need("(");
        String str = getString();
        need(")");
        need(";");
        createDebugOperation(str);
      } else if (ret.equals("are_equal")) {
        need("(");
        String str = need();
        TracedVariable t1 = needVar(str);
        need(",");
        str = need();
        TracedVariable t2 = needVar(str);
        need(")");
        need(";");
        createCheckOperation(t1, t2);
      } else if (ret.equals("print_bdd") || ret.equals("show_bdd")) {
        need("(");
        String str = need();
        TracedVariable v = needVar(str);
        need(")");
        need(";");
        createPrintOperation(ret.equals("show_bdd"), v);
      } else if (ret.equals("save_bdd")) {
        need("(");
        String str = need();
        TracedVariable v = needVar(str);
        need(")");
        need(";");
        createSaveOperation(v);
      } else if (ret.equals("check_point_for_force_reordering")) {
        need("(");
        String str = need();
        int type = Integer.parseInt(str);
        need(")");
        need(";");
        BDDFactory.ReorderMethod m = getReorderMethod(type);
        createReorderOperation(m);
      } else {

        TracedVariable vret = (TracedVariable) map.get(ret);
        if (vret == null) // just used a new variable
        {
          vret = addTemporaryVariable(ret);
        }

        need("=");
        String op = need();

        updateUsage(vret);

        TracedBDDOperation tp = createBDDOperation();
        TracedVariable var = (TracedVariable) map.get(op);

        if (var != null) { // asignment!
          need(";");
          tp.operands.add(var);
          tp.ret = vret;
          tp.op = "=";
          updateUsage(var);
        } else {
          tp.op = op;
          tp.ret = vret;
          if (op.equals("new_int_leaf")) {
            need("(");
            String c = need();
            need(")");
            need(";");
            Object operand = map.get(c); // assuming 0 or 1
            if (operand == null) {
              throw new InternalError();
            }
            tp.operands.add(operand);
            tp.ret = vret;
            tp.op = "=";
          } else {
            String s1, s2;
            need("(");

            do {
              s1 = need();
              tp.operands.add(needVar(s1));
              s1 = need();
            } while (s1.equals(","));
            need(";");
          }
        }

        tp.ops = tp.operands.size();
        if (tp.ops > 0) {
          tp.op1 = (TracedVariable) tp.operands.elementAt(0);
        }
        if (tp.ops > 1) {
          tp.op2 = (TracedVariable) tp.operands.elementAt(1);
        }
        if (tp.ops > 2) {
          tp.op3 = (TracedVariable) tp.operands.elementAt(2);
        }
      }
    }
  }

  private static BDDFactory.ReorderMethod getReorderMethod(int type) {
    BDDFactory.ReorderMethod m;
    switch (type) {
      case 0:
        m = BDDFactory.REORDER_NONE;
        break;
      case 1:
        m = BDDFactory.REORDER_WIN2;
        break;
      case 2:
        m = BDDFactory.REORDER_WIN2ITE;
        break;
      case 3:
        m = BDDFactory.REORDER_WIN3;
        break;
      case 4:
        m = BDDFactory.REORDER_WIN3ITE;
        break;
      case 5:
        m = BDDFactory.REORDER_SIFT;
        break;
      case 6:
        m = BDDFactory.REORDER_SIFTITE;
        break;
      case 7:
        m = BDDFactory.REORDER_RANDOM;
        break;
      default:
        m = BDDFactory.REORDER_NONE;
        break;
    }
    return m;
  }

  // --------------------------------------------------------------------------------------------

  private void read_input() throws IOException {
    boolean interleave = false;
    LinkedList list = new LinkedList();

    need("INPUT");

    for (int i = 0; ; i++) {
      String name = need();
      if (i == 0
          && (name.equals("CURR_NEXT_ASSOCIATE_EVEN_ODD_INPUT_VARS")
              || name.equals("STATE_VAR_ASSOCIATE_CURR_NEXT_INTERLEAVE"))) {
        if (name.equals("STATE_VAR_ASSOCIATE_CURR_NEXT_INTERLEAVE")) {
          interleave = true;
        }
      } else {
        // alloc_var(name);
        list.add(name);

        name = need();
        if (name.equals(";")) {
          break;
        } else if (!name.equals(",")) {
          throw new IOException(
              "expected ',' when reading inputs, but got: " + name + " at line " + line_count);
        }
      }
    }

    int count = list.size();
    setup_bdd(count);
    for (Iterator it = list.iterator(); it.hasNext(); ) {
      String name = (String) it.next();
      alloc_var(name);
    }

    // ------------------------ build permutations
    int size = variables.size();
    // Test.checkEquality( size%2, 0, "odd varcount ??");
    int[] v1 = new int[size / 2];
    int[] v2 = new int[size / 2];

    Enumeration e = variables.elements();
    for (int i = 0; i < (size & ~1); i++) {
      TracedVariable v = (TracedVariable) e.nextElement();
      if (v.bdd.nodeCount() > 1) {
        throw new InternalError();
      }
      if (interleave) {
        if ((i % 2) == 0) {
          v1[i / 2] = v.bdd.var();
        } else {
          v2[i / 2] = v.bdd.var();
        }
      } else {
        if (i < v1.length) {
          v1[i] = v.bdd.var();
        } else {
          v2[i - v1.length] = v.bdd.var();
        }
      }
    }

    s2sp = bdd.makePair();
    s2sp.set(v1, v2);
    sp2s = bdd.makePair();
    sp2s.set(v2, v1);

    bdd.varBlockAll();

    // s2sp.showName();

  }

  private TracedVariable needVar(String str) throws IOException {
    TracedVariable ret = (TracedVariable) map.get(str);
    if (ret == null) {
      throw new IOException("Unknown variable/operand " + str + " at line " + line_count);
    }
    updateUsage(ret);
    return ret;
  }

  private void updateUsage(TracedVariable v) {
    // if(v.last_use != Integer.MAX_VALUE)
    v.last_use = op_count;
  }

  private TracedVariable addTemporaryVariable(String name) {
    TracedVariable vret = new TracedVariable();
    vret.last_use = op_count;
    vret.name = name;
    vret.bdd = bdd.zero(); // nothing...
    variables.add(vret);
    map.put(name, vret);

    return vret;
  }
  // -----------------------------------------------------

  private void need(String what) throws IOException {
    String got = need();
    if (!got.equals(what)) {
      check(false, "Syntax error: expected '" + what + "', but read '" + got + "', op=" + op_count);
    }
  }

  private String need() throws IOException {
    String ret = next();
    if (ret == null) {
      check(false, "pre-mature end of file");
    }
    return ret;
  }
  // -----------------------------------------------------
  private int read() {
    int c = -1;
    if (stack_tos > 0) {
      c = stack[--stack_tos];
    } else {
      try {
        c = is.read();
      } catch (IOException exx) {
      }
    }
    if (c == '\n') {
      line_count++;
    }
    return c;
  }

  private void push(int c) {
    stack[stack_tos++] = c;
    if (c == '\n') {
      line_count--;
    }
  }

  private boolean isSpace(int c) {
    return (c == ' ' || c == '\n' || c == '\t' || c == '\r');
  }

  private boolean isAlnum(int c) {
    return ((c >= '0' && c <= '9')
        || (c >= 'a' && c <= 'z')
        || (c >= 'A' && c <= 'Z')
        || c == '_'
        || c == '-');
  }

  // -----------------------------------------------------

  private String getString() throws IOException {
    StringBuffer buffer = new StringBuffer();
    int c = 0;
    while (isSpace(c = read())) {
    }
    if (c != '"') {
      throw new IOException("Not an string at line " + line_count);
    }

    while ((c = read()) != '"') {
      buffer.append((char) c);
    }

    return buffer.toString();
  }

  private void skip_eol() {
    for (; ; ) {
      int c = read();
      if (c == -1 || c == '\n') {
        return;
      }
    }
  }

  private String next() {
    sb.setLength(0);
    int c;
    do {
      c = read();
      if (c == -1) {
        return null; // EOF
      }
    } while (isSpace(c));

    if (isAlnum(c)) {
      do {
        sb.append((char) c);
        c = read();
        // XXX: error fixed ??? "return" was missing
        if (c == -1) {
          return sb.toString();
        }
      } while (isAlnum(c));

      if (!isSpace(c)) {
        push(c);
      }
    } else {
      if (c == '%' || c == '#') {
        int old_line = line_count;
        if (c == '%') {
          String count = next();
          TracedOperation tp = (TracedOperation) operations.lastElement();
          if (tp.size == -1) {
            tp.size = Integer.parseInt(count);
          }
        }

        if (old_line == line_count) {
          skip_eol(); // haven't had a \n yet
        }
        return next();
      }
      return "" + ((char) c);
    }

    return sb.toString();
  }

  /* package */ void checkEquality(int a, int b, String txt) throws IOException {
    if (a != b) {
      throw new IOException(txt + ", " + a + " != " + b);
    }
  }

  /* package */ void check(boolean b, String txt) throws IOException {
    if (!b) {
      throw new IOException(txt);
    }
  }

  /* package */ void check(boolean b) throws IOException {
    if (!b) {
      throw new IOException("Check failed");
    }
  }

  // ----------------------------------------------------

  public static void main(String[] args) {
    // TraceDriver.verbose = true;

    if (args.length == 0) {
      out.println(
          "Usage:  java "
              + TraceDriver.class.getName()
              + " file.net.sf.javabdd.trace {file2.net.sf.javabdd.trace ...}");
      return;
    }
    int bddnodes =
        Integer.parseInt(System.getProperty("bddnodes", Integer.toString(DEFAULT_NODES)));
    long totalTime = 0;
    try {
      for (int i = 0; i < args.length; ++i) {
        TraceDriver td = new TraceDriver(args[i], bddnodes);
        totalTime += td.time;
      }
      if (args.length > 1) {
        out.println("Total time for all traces: " + totalTime + " [ms]");
      }
    } catch (IOException exx) {
      out.println("FAILED: " + exx.getMessage());
      exx.printStackTrace();
    }
  }
}

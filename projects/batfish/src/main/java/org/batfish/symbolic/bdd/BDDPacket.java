package org.batfish.symbolic.bdd;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.BDDPairing;
import net.sf.javabdd.JFactory;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.Prefix;

/**
 * A collection of attributes describing an packet, represented using BDDs
 *
 * @author Ryan Beckett
 */
public class BDDPacket {

  public static BDDFactory factory;

  private static BDDPairing pairing;

  static {
    factory = JFactory.init(10000, 1000);
    factory.disableReorder();
    factory.setCacheRatio(64);
    // Disables printing
    /*
    try {
      CallbackHandler handler = new CallbackHandler();
      Method m = handler.getClass().getDeclaredMethod("handle", (Class<?>[]) null);
      factory.registerGCCallback(handler, m);
      factory.registerResizeCallback(handler, m);
      factory.registerReorderCallback(handler, m);
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    }
    */
    pairing = factory.makePair();
  }

  private static final int DSCP_LENGTH = 6;

  private static final int ECN_LENGTH = 2;

  private static final int ICMP_CODE_LENGTH = 8;

  private static final int ICMP_TYPE_LENGTH = 8;

  private static final int IP_LENGTH = 32;

  private static final int IP_PROTOCOL_LENGTH = 8;

  private static final int PORT_LENGTH = 16;

  private static final int TCP_FLAG_LENGTH = 1;

  private Map<Integer, String> _bitNames;

  private BDDInteger _dscp;

  private BDDInteger _dstIp;

  private BDDInteger _dstPort;

  private BDDInteger _ecn;

  private BDDInteger _icmpCode;

  private BDDInteger _icmpType;

  private BDDInteger _ipProtocol;

  private BDDInteger _srcIp;

  private BDDInteger _srcPort;

  private BDD _tcpAck;

  private BDD _tcpCwr;

  private BDD _tcpEce;

  private BDD _tcpFin;

  private BDD _tcpPsh;

  private BDD _tcpRst;

  private BDD _tcpSyn;

  private BDD _tcpUrg;

  /*
   * Creates a collection of BDD variables representing the
   * various attributes of a control plane advertisement.
   */
  public BDDPacket() {

    // Make sure we have the right number of variables
    int numVars = factory.varNum();
    int numNeeded =
        IP_LENGTH * 2
            + PORT_LENGTH * 2
            + IP_PROTOCOL_LENGTH
            + ICMP_CODE_LENGTH
            + ICMP_TYPE_LENGTH
            + TCP_FLAG_LENGTH * 8
            + DSCP_LENGTH
            + ECN_LENGTH;
    if (numVars < numNeeded) {
      factory.setVarNum(numNeeded);
    }

    _bitNames = new HashMap<>();

    // Initialize integer values
    int idx = 0;
    _ipProtocol = BDDInteger.makeFromIndex(factory, IP_PROTOCOL_LENGTH, idx, false);
    addBitNames("ipProtocol", IP_PROTOCOL_LENGTH, idx, false);
    idx += IP_PROTOCOL_LENGTH;
    _dstIp = BDDInteger.makeFromIndex(factory, IP_LENGTH, idx, true);
    addBitNames("dstIp", IP_LENGTH, idx, true);
    idx += IP_LENGTH;
    _srcIp = BDDInteger.makeFromIndex(factory, IP_LENGTH, idx, true);
    addBitNames("srcIp", IP_LENGTH, idx, true);
    idx += IP_LENGTH;
    _dstPort = BDDInteger.makeFromIndex(factory, PORT_LENGTH, idx, false);
    addBitNames("dstPort", PORT_LENGTH, idx, false);
    idx += PORT_LENGTH;
    _srcPort = BDDInteger.makeFromIndex(factory, PORT_LENGTH, idx, false);
    addBitNames("srcPort", PORT_LENGTH, idx, false);
    idx += PORT_LENGTH;
    _icmpCode = BDDInteger.makeFromIndex(factory, ICMP_CODE_LENGTH, idx, false);
    addBitNames("icmpCode", ICMP_CODE_LENGTH, idx, false);
    idx += ICMP_CODE_LENGTH;
    _icmpType = BDDInteger.makeFromIndex(factory, ICMP_TYPE_LENGTH, idx, false);
    addBitNames("icmpType", ICMP_TYPE_LENGTH, idx, false);
    idx += ICMP_TYPE_LENGTH;
    _tcpAck = factory.ithVar(idx);
    _bitNames.put(idx, "tcpAck");
    idx += TCP_FLAG_LENGTH;
    _tcpCwr = factory.ithVar(idx);
    _bitNames.put(idx, "tcpCwr");
    idx += TCP_FLAG_LENGTH;
    _tcpEce = factory.ithVar(idx);
    _bitNames.put(idx, "tcpEce");
    idx += TCP_FLAG_LENGTH;
    _tcpFin = factory.ithVar(idx);
    _bitNames.put(idx, "tcpFin");
    idx += TCP_FLAG_LENGTH;
    _tcpPsh = factory.ithVar(idx);
    _bitNames.put(idx, "tcpPsh");
    idx += TCP_FLAG_LENGTH;
    _tcpRst = factory.ithVar(idx);
    _bitNames.put(idx, "tcpRst");
    idx += TCP_FLAG_LENGTH;
    _tcpSyn = factory.ithVar(idx);
    _bitNames.put(idx, "tcpSyn");
    idx += TCP_FLAG_LENGTH;
    _tcpUrg = factory.ithVar(idx);
    _bitNames.put(idx, "tcpUrg");
    idx += TCP_FLAG_LENGTH;
    _dscp = BDDInteger.makeFromIndex(factory, DSCP_LENGTH, idx, false);
    addBitNames("dscp", DSCP_LENGTH, idx, false);
    idx += DSCP_LENGTH;
    _ecn = BDDInteger.makeFromIndex(factory, ECN_LENGTH, idx, false);
    addBitNames("ecn", ECN_LENGTH, idx, false);
  }

  /*
   * Create a BDDRecord from another. Because BDDs are immutable,
   * there is no need for a deep copy.
   */
  private BDDPacket(BDDPacket other) {
    _srcIp = new BDDInteger(other._srcIp);
    _dstIp = new BDDInteger(other._dstIp);
    _srcPort = new BDDInteger(other._srcPort);
    _dstPort = new BDDInteger(other._dstPort);
    _icmpCode = new BDDInteger(other._icmpCode);
    _icmpType = new BDDInteger(other._icmpType);
    _ipProtocol = new BDDInteger(other._ipProtocol);
    _tcpAck = other._tcpAck;
    _tcpCwr = other._tcpCwr;
    _tcpEce = other._tcpEce;
    _tcpFin = other._tcpFin;
    _tcpPsh = other._tcpPsh;
    _tcpRst = other._tcpRst;
    _tcpSyn = other._tcpSyn;
    _tcpUrg = other._tcpUrg;
    _dscp = new BDDInteger(other._dscp);
    _ecn = new BDDInteger(other._ecn);
  }

  /*
   * Helper function that builds a map from BDD variable index
   * to some more meaningful name. Helpful for debugging.
   */
  private void addBitNames(String s, int length, int index, boolean reverse) {
    for (int i = index; i < index + length; i++) {
      if (reverse) {
        _bitNames.put(i, s + (length - 1 - (i - index)));
      } else {
        _bitNames.put(i, s + (i - index + 1));
      }
    }
  }

  /*
   * Convenience method for the copy constructor
   */
  public BDDPacket copy() {
    return new BDDPacket(this);
  }

  /*
   * Converts a BDD to the graphviz DOT format for debugging.
   */
  public String dot(BDD bdd) {
    StringBuilder sb = new StringBuilder();
    sb.append("digraph G {\n");
    sb.append("0 [shape=box, label=\"0\", style=filled, shape=box, height=0.3, width=0.3];\n");
    sb.append("1 [shape=box, label=\"1\", style=filled, shape=box, height=0.3, width=0.3];\n");
    dotRec(sb, bdd, new HashSet<>());
    sb.append("}");
    return sb.toString();
  }

  /*
   * Creates a unique id for a bdd node when generating
   * a DOT file for graphviz
   */
  private Integer dotId(BDD bdd) {
    if (bdd.isZero()) {
      return 0;
    }
    if (bdd.isOne()) {
      return 1;
    }
    return bdd.hashCode() + 2;
  }

  /*
   * Recursively builds each of the intermediate BDD nodes in the
   * graphviz DOT format.
   */
  private void dotRec(StringBuilder sb, BDD bdd, Set<BDD> visited) {
    if (bdd.isOne() || bdd.isZero() || visited.contains(bdd)) {
      return;
    }
    int val = dotId(bdd);
    int valLow = dotId(bdd.low());
    int valHigh = dotId(bdd.high());
    String name = _bitNames.get(bdd.var());
    sb.append(val).append(" [label=\"").append(name).append("\"]\n");
    sb.append(val).append(" -> ").append(valLow).append("[style=dotted]\n");
    sb.append(val).append(" -> ").append(valHigh).append("[style=filled]\n");
    visited.add(bdd);
    dotRec(sb, bdd.low(), visited);
    dotRec(sb, bdd.high(), visited);
  }

  /**
   * @param bdd a BDD representing a set of packet headers
   * @return A Flow.Builder for a representative of the set, if it's non-empty
   */
  public Optional<Flow.Builder> getFlow(BDD bdd) {
    BDD satAssignment = bdd.fullSatOne();
    if (satAssignment.isZero()) {
      return Optional.empty();
    }

    Flow.Builder fb = Flow.builder();
    fb.setDstIp(new Ip(_dstIp.satAssignmentToLong(satAssignment)));
    fb.setSrcIp(new Ip(_srcIp.satAssignmentToLong(satAssignment)));
    fb.setDstPort(_dstPort.satAssignmentToLong(satAssignment).intValue());
    fb.setSrcPort(_srcPort.satAssignmentToLong(satAssignment).intValue());
    fb.setIpProtocol(
        IpProtocol.fromNumber(_ipProtocol.satAssignmentToLong(satAssignment).intValue()));
    fb.setIcmpCode(_icmpCode.satAssignmentToLong(satAssignment).intValue());
    fb.setIcmpType(_icmpType.satAssignmentToLong(satAssignment).intValue());
    fb.setTcpFlagsAck(_tcpAck.and(satAssignment).isZero() ? 0 : 1);
    fb.setTcpFlagsCwr(_tcpCwr.and(satAssignment).isZero() ? 0 : 1);
    fb.setTcpFlagsEce(_tcpEce.and(satAssignment).isZero() ? 0 : 1);
    fb.setTcpFlagsFin(_tcpFin.and(satAssignment).isZero() ? 0 : 1);
    fb.setTcpFlagsPsh(_tcpPsh.and(satAssignment).isZero() ? 0 : 1);
    fb.setTcpFlagsRst(_tcpRst.and(satAssignment).isZero() ? 0 : 1);
    fb.setTcpFlagsSyn(_tcpSyn.and(satAssignment).isZero() ? 0 : 1);
    fb.setTcpFlagsUrg(_tcpUrg.and(satAssignment).isZero() ? 0 : 1);
    fb.setDscp(_dscp.satAssignmentToLong(satAssignment).intValue());
    fb.setEcn(_ecn.satAssignmentToLong(satAssignment).intValue());
    return Optional.of(fb);
  }

  public BDDInteger getDscp() {
    return _dscp;
  }

  public void setDscp(BDDInteger x) {
    this._dscp = x;
  }

  public BDDInteger getDstIp() {
    return _dstIp;
  }

  public void setDstIp(BDDInteger x) {
    this._dstIp = x;
  }

  public BDDInteger getDstPort() {
    return _dstPort;
  }

  public void setDstPort(BDDInteger x) {
    this._dstPort = x;
  }

  public BDDInteger getEcn() {
    return _ecn;
  }

  public void setEcn(BDDInteger x) {
    this._ecn = x;
  }

  public BDDInteger getIcmpCode() {
    return _icmpCode;
  }

  public void setIcmpCode(BDDInteger x) {
    this._icmpCode = x;
  }

  public BDDInteger getIcmpType() {
    return _icmpType;
  }

  public void setIcmpType(BDDInteger x) {
    this._icmpType = x;
  }

  public BDDInteger getIpProtocol() {
    return _ipProtocol;
  }

  public void setIpProtocol(BDDInteger x) {
    this._ipProtocol = x;
  }

  public BDDInteger getSrcIp() {
    return _srcIp;
  }

  public void setSrcIp(BDDInteger x) {
    this._srcIp = x;
  }

  public BDDInteger getSrcPort() {
    return _srcPort;
  }

  public void setSrcPort(BDDInteger x) {
    this._srcPort = x;
  }

  public BDD getTcpAck() {
    return _tcpAck;
  }

  public void setTcpAck(BDD tcpAck) {
    this._tcpAck = tcpAck;
  }

  public BDD getTcpCwr() {
    return _tcpCwr;
  }

  public void setTcpCwr(BDD tcpCwr) {
    this._tcpCwr = tcpCwr;
  }

  public BDD getTcpEce() {
    return _tcpEce;
  }

  public void setTcpEce(BDD tcpEce) {
    this._tcpEce = tcpEce;
  }

  public BDD getTcpFin() {
    return _tcpFin;
  }

  public void setTcpFin(BDD tcpFin) {
    this._tcpFin = tcpFin;
  }

  public BDD getTcpPsh() {
    return _tcpPsh;
  }

  public void setTcpPsh(BDD tcpPsh) {
    this._tcpPsh = tcpPsh;
  }

  public BDD getTcpRst() {
    return _tcpRst;
  }

  public void setTcpRst(BDD tcpRst) {
    this._tcpRst = tcpRst;
  }

  public BDD getTcpSyn() {
    return _tcpSyn;
  }

  public void setTcpSyn(BDD tcpSyn) {
    this._tcpSyn = tcpSyn;
  }

  public BDD getTcpUrg() {
    return _tcpUrg;
  }

  public void setTcpUrg(BDD tcpUrg) {
    this._tcpUrg = tcpUrg;
  }

  @Override
  public int hashCode() {
    int result = _dstIp != null ? _dstIp.hashCode() : 0;
    result = 31 * result + (_srcIp != null ? _srcIp.hashCode() : 0);
    result = 31 * result + (_dstPort != null ? _dstPort.hashCode() : 0);
    result = 31 * result + (_srcPort != null ? _srcPort.hashCode() : 0);
    result = 31 * result + (_icmpCode != null ? _icmpCode.hashCode() : 0);
    result = 31 * result + (_icmpType != null ? _icmpType.hashCode() : 0);
    result = 31 * result + (_ipProtocol != null ? _ipProtocol.hashCode() : 0);
    result = 31 * result + (_tcpAck != null ? _tcpAck.hashCode() : 0);
    result = 31 * result + (_tcpCwr != null ? _tcpCwr.hashCode() : 0);
    result = 31 * result + (_tcpEce != null ? _tcpEce.hashCode() : 0);
    result = 31 * result + (_tcpFin != null ? _tcpFin.hashCode() : 0);
    result = 31 * result + (_tcpPsh != null ? _tcpPsh.hashCode() : 0);
    result = 31 * result + (_tcpRst != null ? _tcpRst.hashCode() : 0);
    result = 31 * result + (_tcpSyn != null ? _tcpSyn.hashCode() : 0);
    result = 31 * result + (_tcpUrg != null ? _tcpUrg.hashCode() : 0);
    result = 31 * result + (_dscp != null ? _dscp.hashCode() : 0);
    result = 31 * result + (_ecn != null ? _ecn.hashCode() : 0);
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof BDDPacket)) {
      return false;
    }
    BDDPacket other = (BDDPacket) o;

    return Objects.equals(_srcPort, other._srcPort)
        && Objects.equals(_icmpType, other._icmpType)
        && Objects.equals(_icmpCode, other._icmpCode)
        && Objects.equals(_ipProtocol, other._ipProtocol)
        && Objects.equals(_dstPort, other._dstPort)
        && Objects.equals(_dstIp, other._dstIp)
        && Objects.equals(_srcIp, other._srcIp)
        && Objects.equals(_tcpAck, other._tcpAck)
        && Objects.equals(_tcpCwr, other._tcpCwr)
        && Objects.equals(_tcpEce, other._tcpEce)
        && Objects.equals(_tcpFin, other._tcpFin)
        && Objects.equals(_tcpPsh, other._tcpPsh)
        && Objects.equals(_tcpRst, other._tcpRst)
        && Objects.equals(_tcpSyn, other._tcpSyn)
        && Objects.equals(_tcpUrg, other._tcpUrg)
        && Objects.equals(_dscp, other._dscp)
        && Objects.equals(_ecn, other._ecn);
  }

  public BDD restrict(BDD bdd, Prefix pfx) {
    int len = pfx.getPrefixLength();
    long bits = pfx.getStartIp().asLong();
    int[] vars = new int[len];
    BDD[] vals = new BDD[len];
    pairing.reset();
    for (int i = 0; i < len; i++) {
      int var = _dstIp.getBitvec()[i].var(); // dstIpIndex + i;
      BDD subst = Ip.getBitAtPosition(bits, i) ? factory.one() : factory.zero();
      vars[i] = var;
      vals[i] = subst;
    }
    pairing.set(vars, vals);
    return bdd.veccompose(pairing);
  }

  public BDD restrict(BDD bdd, List<Prefix> prefixes) {
    if (prefixes.isEmpty()) {
      throw new BatfishException("Empty prefix list in BDDRecord restrict");
    }
    BDD r = restrict(bdd, prefixes.get(0));
    for (int i = 1; i < prefixes.size(); i++) {
      Prefix p = prefixes.get(i);
      BDD x = restrict(bdd, p);
      r = r.or(x);
    }
    return r;
  }
}

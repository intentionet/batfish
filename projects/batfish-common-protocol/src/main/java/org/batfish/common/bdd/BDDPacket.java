package org.batfish.common.bdd;

import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.common.bdd.BDDInteger.makeFromIndex;
import static org.batfish.common.bdd.BDDUtils.isAssignment;
import static org.batfish.common.bdd.BDDUtils.swapPairing;

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
import org.batfish.common.bdd.BDDFlowConstraintGenerator.FlowPreference;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowState;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;

/**
 * A collection of attributes describing an packet, represented using BDDs
 *
 * @author Ryan Beckett
 */
public class BDDPacket {

  /*
   * Initial size of the BDD factory node table. Automatically resized as needed. Increasing this
   * will reduce time spent garbage collecting for large computations, but will waste memory for
   * smaller ones.
   */
  private static final int JFACTORY_INITIAL_NODE_TABLE_SIZE = 10000;

  /*
   * The ratio of node table size to node cache size to preserve when resizing. The default
   * value is 0, which means never resize the cache.
   */
  private static final int JFACTORY_CACHE_RATIO = 32;

  /*
   * Initial size of the BDD factory node cache. Automatically resized when the node table is,
   * to preserve the cache ratio.
   */
  private static final int JFACTORY_INITIAL_NODE_CACHE_SIZE =
      (JFACTORY_INITIAL_NODE_TABLE_SIZE + JFACTORY_CACHE_RATIO - 1) / JFACTORY_CACHE_RATIO;

  private static final int DSCP_LENGTH = 6;

  private static final int ECN_LENGTH = 2;

  private static final int FRAGMENT_OFFSET_LENGTH = 13;

  private static final int ICMP_CODE_LENGTH = 8;

  private static final int ICMP_TYPE_LENGTH = 8;

  private static final int IP_LENGTH = 32;

  private static final int IP_PROTOCOL_LENGTH = 8;

  private static final int PORT_LENGTH = 16;

  private static final int STATE_LENGTH = 2;

  private static final int TCP_FLAG_LENGTH = 1;

  private Map<Integer, String> _bitNames;

  private BDDInteger _dscp;

  private BDDInteger _dstIp;

  private BDDInteger _dstPort;

  private BDDInteger _ecn;

  private final BDDFactory _factory;

  private BDDInteger _fragmentOffset;

  private BDDIcmpCode _icmpCode;

  private BDDIcmpType _icmpType;

  private BDDIpProtocol _ipProtocol;

  private int _nextFreeBDDVarIdx = 0;

  private final BDDPairing _pairing;

  private BDDInteger _srcIp;

  private BDDInteger _srcPort;

  private BDDInteger _state;

  private BDD _tcpAck;

  private BDD _tcpCwr;

  private BDD _tcpEce;

  private BDD _tcpFin;

  private BDD _tcpPsh;

  private BDD _tcpRst;

  private BDD _tcpSyn;

  private BDD _tcpUrg;

  private final BDDPairing _swapSourceAndDestinationPairing;

  // Picking representative flows
  private final BDDRepresentativePicker _picker;

  /*
   * Creates a collection of BDD variables representing the
   * various attributes of a control plane advertisement.
   */
  public BDDPacket() {
    _factory = JFactory.init(JFACTORY_INITIAL_NODE_TABLE_SIZE, JFACTORY_INITIAL_NODE_CACHE_SIZE);
    _factory.enableReorder();
    _factory.setCacheRatio(JFACTORY_CACHE_RATIO);
    // Do not impose a maximum node table increase
    _factory.setMaxIncrease(0);
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
    _pairing = _factory.makePair();

    // Make sure we have the right number of variables
    int numVars = _factory.varNum();
    int numNeeded =
        IP_LENGTH * 2
            + PORT_LENGTH * 2
            + IP_PROTOCOL_LENGTH
            + ICMP_CODE_LENGTH
            + ICMP_TYPE_LENGTH
            + TCP_FLAG_LENGTH * 8
            + DSCP_LENGTH
            + ECN_LENGTH
            + FRAGMENT_OFFSET_LENGTH
            + STATE_LENGTH;
    if (numVars < numNeeded) {
      _factory.setVarNum(numNeeded);
    }

    _bitNames = new HashMap<>();

    _dstIp = allocateBDDInteger("dstIp", IP_LENGTH, false);
    _srcIp = allocateBDDInteger("srcIp", IP_LENGTH, false);
    _dstPort = allocateBDDInteger("dstPort", PORT_LENGTH, false);
    _srcPort = allocateBDDInteger("srcPort", PORT_LENGTH, false);
    _ipProtocol = new BDDIpProtocol(allocateBDDInteger("ipProtocol", IP_PROTOCOL_LENGTH, false));
    _icmpCode = new BDDIcmpCode(allocateBDDInteger("icmpCode", ICMP_CODE_LENGTH, false));
    _icmpType = new BDDIcmpType(allocateBDDInteger("icmpType", ICMP_TYPE_LENGTH, false));
    _tcpAck = allocateBDDBit("tcpAck");
    _tcpCwr = allocateBDDBit("tcpCwr");
    _tcpEce = allocateBDDBit("tcpEce");
    _tcpFin = allocateBDDBit("tcpFin");
    _tcpPsh = allocateBDDBit("tcpPsh");
    _tcpRst = allocateBDDBit("tcpRst");
    _tcpSyn = allocateBDDBit("tcpSyn");
    _tcpUrg = allocateBDDBit("tcpUrg");
    _dscp = allocateBDDInteger("dscp", DSCP_LENGTH, false);
    _ecn = allocateBDDInteger("ecn", ECN_LENGTH, false);
    _fragmentOffset = allocateBDDInteger("fragmentOffset", FRAGMENT_OFFSET_LENGTH, false);
    _state = allocateBDDInteger("state", STATE_LENGTH, false);
    _swapSourceAndDestinationPairing =
        swapPairing(
            getDstIp(), getSrcIp(), //
            getDstPort(), getSrcPort());

    _picker =
        new BDDRepresentativePicker(
            new BDDFlowConstraintGenerator(this).generateFlowPreference(FlowPreference.DEBUGGING));
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

  /**
   * Allocate a new single-bit {@link BDD} variable.
   *
   * @param name Used for debugging.
   * @return A {@link BDD} representing the sentence "this variable is true" for the new variable.
   */
  public BDD allocateBDDBit(String name) {
    if (_factory.varNum() < _nextFreeBDDVarIdx + 1) {
      _factory.setVarNum(_nextFreeBDDVarIdx + 1);
    }
    _bitNames.put(_nextFreeBDDVarIdx, name);
    BDD bdd = _factory.ithVar(_nextFreeBDDVarIdx);
    _nextFreeBDDVarIdx++;
    return bdd;
  }

  /**
   * Allocate a new {@link BDDInteger} variable.
   *
   * @param name Used for debugging.
   * @param bits The number of bits to allocate.
   * @param reverse If true, reverse the BDD order of the bits.
   * @return The new variable.
   */
  public BDDInteger allocateBDDInteger(String name, int bits, boolean reverse) {
    if (_factory.varNum() < _nextFreeBDDVarIdx + bits) {
      _factory.setVarNum(_nextFreeBDDVarIdx + bits);
    }
    BDDInteger var = makeFromIndex(_factory, bits, _nextFreeBDDVarIdx, reverse);
    addBitNames(name, STATE_LENGTH, _nextFreeBDDVarIdx, false);
    _nextFreeBDDVarIdx += bits;
    return var;
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

  /** @return The {@link BDDFactory} used by this packet. */
  public BDDFactory getFactory() {
    return _factory;
  }

  /**
   * Get a representative flow in a BDD. First, try to get an ICMP echo request flow; second, try to
   * get a UDP flow used for traceroute; third, try to get a TCP flow with a named port; finally try
   * to get an arbitrary one.
   *
   * @param bdd a BDD representing a set of packet headers
   * @return A Flow.Builder for a representative of the set, if it's non-empty
   */
  public Optional<Flow.Builder> getFlow(BDD bdd) {
    BDD representativeBDD = _picker.pickRepresentative(bdd);
    if (representativeBDD.isZero()) {
      return Optional.empty();
    }
    return Optional.of(getFlowFromAssignment(representativeBDD));
  }

  public Flow.Builder getFlowFromAssignment(BDD satAssignment) {
    checkArgument(isAssignment(satAssignment));

    Flow.Builder fb = Flow.builder();
    fb.setDstIp(Ip.create(_dstIp.satAssignmentToLong(satAssignment)));
    fb.setSrcIp(Ip.create(_srcIp.satAssignmentToLong(satAssignment)));
    fb.setDstPort(_dstPort.satAssignmentToLong(satAssignment).intValue());
    fb.setSrcPort(_srcPort.satAssignmentToLong(satAssignment).intValue());
    fb.setIpProtocol(_ipProtocol.satAssignmentToValue(satAssignment));
    fb.setIcmpCode(_icmpCode.satAssignmentToValue(satAssignment));
    fb.setIcmpType(_icmpType.satAssignmentToValue(satAssignment));
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
    fb.setFragmentOffset(_fragmentOffset.satAssignmentToLong(satAssignment).intValue());
    fb.setState(FlowState.fromNum(_state.satAssignmentToLong(satAssignment).intValue()));
    return fb;
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

  public BDDInteger getFragmentOffset() {
    return _fragmentOffset;
  }

  public void setFragmentOffset(BDDInteger x) {
    this._fragmentOffset = x;
  }

  public BDDIcmpCode getIcmpCode() {
    return _icmpCode;
  }

  public void setIcmpCode(BDDIcmpCode x) {
    this._icmpCode = x;
  }

  public BDDIcmpType getIcmpType() {
    return _icmpType;
  }

  public void setIcmpType(BDDIcmpType x) {
    this._icmpType = x;
  }

  public BDDIpProtocol getIpProtocol() {
    return _ipProtocol;
  }

  public void setIpProtocol(BDDIpProtocol x) {
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

  public BDDInteger getState() {
    return _state;
  }

  public void setState(BDDInteger x) {
    this._state = x;
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
    result = 31 * result + (_fragmentOffset != null ? _fragmentOffset.hashCode() : 0);
    result = 31 * result + (_state != null ? _state.hashCode() : 0);
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
        && Objects.equals(_ecn, other._ecn)
        && Objects.equals(_fragmentOffset, other._fragmentOffset)
        && Objects.equals(_state, other._state);
  }

  public BDD restrict(BDD bdd, Prefix pfx) {
    int len = pfx.getPrefixLength();
    long bits = pfx.getStartIp().asLong();
    int[] vars = new int[len];
    BDD[] vals = new BDD[len];
    _pairing.reset();
    for (int i = 0; i < len; i++) {
      int var = _dstIp.getBitvec()[i].var(); // dstIpIndex + i;
      BDD subst = Ip.getBitAtPosition(bits, i) ? _factory.one() : _factory.zero();
      vars[i] = var;
      vals[i] = subst;
    }
    _pairing.set(vars, vals);
    return bdd.veccompose(_pairing);
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

  public BDD swapSourceAndDestinationFields(BDD bdd) {
    return bdd.replace(_swapSourceAndDestinationPairing);
  }
}

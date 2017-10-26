package org.batfish.datamodel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class Header6Space implements Serializable {

  /** */
  private static final long serialVersionUID = 1L;

  private static boolean rangesContain(Collection<SubRange> ranges, int num) {
    for (SubRange range : ranges) {
      if (range.getStart() <= num && num <= range.getEnd()) {
        return true;
      }
    }
    return false;
  }

  private static boolean wildcardsContain(Collection<Ip6Wildcard> wildcards, Ip6 ip) {
    for (Ip6Wildcard wildcard : wildcards) {
      if (wildcard.contains(ip)) {
        return true;
      }
    }
    return false;
  }

  private SortedSet<Integer> _dscps;

  private SortedSet<Ip6Wildcard> _dstIps;

  private SortedSet<SubRange> _dstPorts;

  private SortedSet<Integer> _ecns;

  private SortedSet<SubRange> _fragmentOffsets;

  private SortedSet<SubRange> _icmpCodes;

  private SortedSet<SubRange> _icmpTypes;

  private Set<IpProtocol> _ipProtocols;

  private boolean _negate;

  private SortedSet<Integer> _notDscps;

  private SortedSet<Ip6Wildcard> _notDstIps;

  private SortedSet<SubRange> _notDstPorts;

  private SortedSet<Integer> _notEcns;

  private SortedSet<SubRange> _notFragmentOffsets;

  private SortedSet<SubRange> _notIcmpCodes;

  private SortedSet<SubRange> _notIcmpTypes;

  private Set<IpProtocol> _notIpProtocols;

  private SortedSet<Ip6Wildcard> _notSrcIps;

  private SortedSet<SubRange> _notSrcPorts;

  private SortedSet<Ip6Wildcard> _srcIps;

  private SortedSet<Ip6Wildcard> _srcOrDstIps;

  private SortedSet<SubRange> _srcOrDstPorts;

  private SortedSet<SubRange> _srcPorts;

  private Set<State> _states;

  private List<TcpFlags> _tcpFlags;

  public Header6Space() {
    _dscps = new TreeSet<>();
    _dstIps = new TreeSet<>();
    _dstPorts = new TreeSet<>();
    _ecns = new TreeSet<>();
    _fragmentOffsets = new TreeSet<>();
    _ipProtocols = EnumSet.noneOf(IpProtocol.class);
    _srcIps = new TreeSet<>();
    _srcOrDstIps = new TreeSet<>();
    _srcOrDstPorts = new TreeSet<>();
    _srcPorts = new TreeSet<>();
    _icmpTypes = new TreeSet<>();
    _icmpCodes = new TreeSet<>();
    _states = EnumSet.noneOf(State.class);
    _tcpFlags = new ArrayList<>();
    _notDscps = new TreeSet<>();
    _notDstIps = new TreeSet<>();
    _notDstPorts = new TreeSet<>();
    _notEcns = new TreeSet<>();
    _notFragmentOffsets = new TreeSet<>();
    _notIcmpCodes = new TreeSet<>();
    _notIcmpTypes = new TreeSet<>();
    _notIpProtocols = EnumSet.noneOf(IpProtocol.class);
    _notSrcIps = new TreeSet<>();
    _notSrcPorts = new TreeSet<>();
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof Header6Space)) {
      return false;
    }
    Header6Space other = (Header6Space) o;
    if (!_dscps.equals(other._dscps)) {
      return false;
    }
    if (!_dstIps.equals(other._dstIps)) {
      return false;
    }
    if (!_dstPorts.equals(other._dstPorts)) {
      return false;
    }
    if (!_ecns.equals(other._ecns)) {
      return false;
    }
    if (!_fragmentOffsets.equals(other._fragmentOffsets)) {
      return false;
    }
    if (!_icmpCodes.equals(other._icmpCodes)) {
      return false;
    }
    if (!_icmpTypes.equals(other._icmpTypes)) {
      return false;
    }
    if (!_ipProtocols.equals(other._ipProtocols)) {
      return false;
    }
    if (_negate != other._negate) {
      return false;
    }
    if (!_notDscps.equals(other._notDscps)) {
      return false;
    }
    if (!_notDstIps.equals(other._notDstIps)) {
      return false;
    }
    if (!_notDstPorts.equals(other._notDstPorts)) {
      return false;
    }
    if (!_notEcns.equals(other._notEcns)) {
      return false;
    }
    if (!_notFragmentOffsets.equals(other._notFragmentOffsets)) {
      return false;
    }
    if (!_notIcmpCodes.equals(other._notIcmpCodes)) {
      return false;
    }
    if (!_notIcmpTypes.equals(other._notIcmpTypes)) {
      return false;
    }
    if (!_notIpProtocols.equals(other._notIpProtocols)) {
      return false;
    }
    if (!_notSrcIps.equals(other._notSrcIps)) {
      return false;
    }
    if (!_notSrcPorts.equals(other._notSrcPorts)) {
      return false;
    }
    if (!_srcIps.equals(other._srcIps)) {
      return false;
    }
    if (!_srcOrDstIps.equals(other._srcOrDstIps)) {
      return false;
    }
    if (!_srcOrDstPorts.equals(other._srcOrDstPorts)) {
      return false;
    }
    if (!_srcPorts.equals(other._srcPorts)) {
      return false;
    }
    if (!_states.equals(other._states)) {
      return false;
    }
    if (!_tcpFlags.equals(other._tcpFlags)) {
      return false;
    }
    return true;
  }

  public SortedSet<Integer> getDscps() {
    return _dscps;
  }

  public SortedSet<Ip6Wildcard> getDstIps() {
    return _dstIps;
  }

  public SortedSet<SubRange> getDstPorts() {
    return _dstPorts;
  }

  public SortedSet<Integer> getEcns() {
    return _ecns;
  }

  public SortedSet<SubRange> getFragmentOffsets() {
    return _fragmentOffsets;
  }

  public SortedSet<SubRange> getIcmpCodes() {
    return _icmpCodes;
  }

  public SortedSet<SubRange> getIcmpTypes() {
    return _icmpTypes;
  }

  public Set<IpProtocol> getIpProtocols() {
    return _ipProtocols;
  }

  public boolean getNegate() {
    return _negate;
  }

  public SortedSet<Integer> getNotDscps() {
    return _notDscps;
  }

  public SortedSet<Ip6Wildcard> getNotDstIps() {
    return _notDstIps;
  }

  public SortedSet<SubRange> getNotDstPorts() {
    return _notDstPorts;
  }

  public SortedSet<Integer> getNotEcns() {
    return _notEcns;
  }

  public SortedSet<SubRange> getNotFragmentOffsets() {
    return _notFragmentOffsets;
  }

  public SortedSet<SubRange> getNotIcmpCodes() {
    return _notIcmpCodes;
  }

  public SortedSet<SubRange> getNotIcmpTypes() {
    return _notIcmpTypes;
  }

  public Set<IpProtocol> getNotIpProtocols() {
    return _notIpProtocols;
  }

  public SortedSet<Ip6Wildcard> getNotSrcIps() {
    return _notSrcIps;
  }

  public SortedSet<SubRange> getNotSrcPorts() {
    return _notSrcPorts;
  }

  public SortedSet<Ip6Wildcard> getSrcIps() {
    return _srcIps;
  }

  public SortedSet<Ip6Wildcard> getSrcOrDstIps() {
    return _srcOrDstIps;
  }

  public SortedSet<SubRange> getSrcOrDstPorts() {
    return _srcOrDstPorts;
  }

  public SortedSet<SubRange> getSrcPorts() {
    return _srcPorts;
  }

  public Set<State> getStates() {
    return _states;
  }

  public List<TcpFlags> getTcpFlags() {
    return _tcpFlags;
  }

  @Override
  public int hashCode() {
    // TODO: implement better hashcode
    return 0;
  }

  public boolean matches(Flow6 flow) {
    if (!_dscps.isEmpty() && !_dscps.contains(flow.getDscp())) {
      return false;
    }
    if (!_notDscps.isEmpty() && _notDscps.contains(flow.getDscp())) {
      return false;
    }
    if (!_dstIps.isEmpty() && !wildcardsContain(_dstIps, flow.getDstIp())) {
      return false;
    }
    if (!_notDstIps.isEmpty() && wildcardsContain(_notDstIps, flow.getDstIp())) {
      return false;
    }
    if (!_dstPorts.isEmpty() && !rangesContain(_dstPorts, flow.getDstPort())) {
      return false;
    }
    if (!_notDstPorts.isEmpty() && rangesContain(_notDstPorts, flow.getDstPort())) {
      return false;
    }
    if (!_fragmentOffsets.isEmpty() && !rangesContain(_fragmentOffsets, flow.getFragmentOffset())) {
      return false;
    }
    if (!_notFragmentOffsets.isEmpty()
        && rangesContain(_notFragmentOffsets, flow.getFragmentOffset())) {
      return false;
    }
    if (!_icmpCodes.isEmpty() && !rangesContain(_icmpCodes, flow.getIcmpCode())) {
      return false;
    }
    if (!_notIcmpCodes.isEmpty() && rangesContain(_notIcmpCodes, flow.getFragmentOffset())) {
      return false;
    }
    if (!_icmpTypes.isEmpty() && !rangesContain(_icmpTypes, flow.getIcmpType())) {
      return false;
    }
    if (!_notIcmpTypes.isEmpty() && rangesContain(_notIcmpTypes, flow.getFragmentOffset())) {
      return false;
    }
    if (!_ipProtocols.isEmpty() && !_ipProtocols.contains(flow.getIpProtocol())) {
      return false;
    }
    if (!_notIpProtocols.isEmpty() && _notIpProtocols.contains(flow.getIpProtocol())) {
      return false;
    }
    if (!_srcOrDstIps.isEmpty()
        && !(wildcardsContain(_srcOrDstIps, flow.getSrcIp())
            || wildcardsContain(_srcOrDstIps, flow.getDstIp()))) {
      return false;
    }
    if (!_srcOrDstPorts.isEmpty()
        && !(rangesContain(_srcOrDstPorts, flow.getSrcPort())
            || rangesContain(_srcOrDstPorts, flow.getDstPort()))) {
      return false;
    }
    if (!_srcIps.isEmpty() && !wildcardsContain(_srcIps, flow.getSrcIp())) {
      return false;
    }
    if (!_notSrcIps.isEmpty() && wildcardsContain(_notSrcIps, flow.getSrcIp())) {
      return false;
    }
    if (!_srcPorts.isEmpty() && !rangesContain(_srcPorts, flow.getSrcPort())) {
      return false;
    }
    if (!_notSrcPorts.isEmpty() && rangesContain(_notSrcPorts, flow.getSrcPort())) {
      return false;
    }
    if (!_states.isEmpty() && !_states.contains(flow.getState())) {
      return false;
    }
    if (!_tcpFlags.isEmpty() && !_tcpFlags.stream().anyMatch(tcpFlags -> tcpFlags.match(flow))) {
      return false;
    }
    return true;
  }

  public void setDscps(SortedSet<Integer> dscps) {
    _dscps = dscps;
  }

  public void setDstIps(SortedSet<Ip6Wildcard> dstIps) {
    _dstIps = dstIps;
  }

  public void setDstPorts(SortedSet<SubRange> dstPorts) {
    _dstPorts = dstPorts;
  }

  public void setEcns(SortedSet<Integer> ecns) {
    _ecns = ecns;
  }

  public void setFragmentOffsets(SortedSet<SubRange> fragmentOffsets) {
    _fragmentOffsets = fragmentOffsets;
  }

  public void setIcmpCodes(SortedSet<SubRange> icmpCodes) {
    _icmpCodes = icmpCodes;
  }

  public void setIcmpTypes(SortedSet<SubRange> icmpTypes) {
    _icmpTypes = icmpTypes;
  }

  public void setIpProtocols(Set<IpProtocol> ipProtocols) {
    _ipProtocols.clear();
    _ipProtocols.addAll(ipProtocols);
  }

  public void setNegate(boolean negate) {
    _negate = negate;
  }

  public void setNotDscps(SortedSet<Integer> notDscps) {
    _notDscps = notDscps;
  }

  public void setNotDstIps(SortedSet<Ip6Wildcard> notDstIps) {
    _notDstIps = notDstIps;
  }

  public void setNotDstPorts(SortedSet<SubRange> notDstPorts) {
    _notDstPorts = notDstPorts;
  }

  public void setNotEcns(SortedSet<Integer> notEcns) {
    _notEcns = notEcns;
  }

  public void setNotFragmentOffsets(SortedSet<SubRange> notFragmentOffsets) {
    _notFragmentOffsets = notFragmentOffsets;
  }

  public void setNotIcmpCodes(SortedSet<SubRange> notIcmpCodes) {
    _notIcmpCodes = notIcmpCodes;
  }

  public void setNotIcmpTypes(SortedSet<SubRange> notIcmpTypes) {
    _notIcmpTypes = notIcmpTypes;
  }

  public void setNotIpProtocols(Set<IpProtocol> notIpProtocols) {
    _notIpProtocols.clear();
    _notIpProtocols.addAll(notIpProtocols);
  }

  public void setNotSrcIps(SortedSet<Ip6Wildcard> notSrcIps) {
    _notSrcIps = notSrcIps;
  }

  public void setNotSrcPorts(SortedSet<SubRange> notSrcPorts) {
    _notSrcPorts = notSrcPorts;
  }

  public void setSrcIps(SortedSet<Ip6Wildcard> srcIps) {
    _srcIps = srcIps;
  }

  public void setSrcOrDstIps(SortedSet<Ip6Wildcard> srcOrDstIps) {
    _srcOrDstIps = srcOrDstIps;
  }

  public void setSrcOrDstPorts(SortedSet<SubRange> srcOrDstPorts) {
    _srcOrDstPorts = srcOrDstPorts;
  }

  public void setSrcPorts(SortedSet<SubRange> srcPorts) {
    _srcPorts = srcPorts;
  }

  public void setStates(Set<State> states) {
    _states = states;
  }

  public void setTcpFlags(List<TcpFlags> tcpFlags) {
    _tcpFlags = tcpFlags;
  }

  @Override
  public String toString() {
    return "[Protocols:"
        + _ipProtocols
        + ", SrcIps:"
        + _srcIps
        + ", NotSrcIps:"
        + _notSrcIps
        + ", DstIps:"
        + _dstIps
        + ", NotDstIps:"
        + _notDstIps
        + ", SrcOrDstIps:"
        + _srcOrDstIps
        + ", SrcPorts:"
        + _srcPorts
        + ", NotSrcPorts:"
        + _notSrcPorts
        + ", DstPorts:"
        + _dstPorts
        + ", NotDstPorts:"
        + _notDstPorts
        + ", SrcOrDstPorts:"
        + _srcOrDstPorts
        + ", Dscps: "
        + _dscps
        + ", NotDscps: "
        + _notDscps
        + ", Ecns: "
        + _ecns
        + ", NotEcns: "
        + _notEcns
        + ", FragmentOffsets: "
        + _fragmentOffsets
        + ", NotFragmentOffsets: "
        + _notFragmentOffsets
        + ", IcmpType:"
        + _icmpTypes
        + ", NotIcmpType:"
        + _notIcmpTypes
        + ", IcmpCode:"
        + _icmpCodes
        + ", NotIcmpCode:"
        + _notIcmpCodes
        + ", States:"
        + _states
        + ", TcpFlags:"
        + _tcpFlags
        + "]";
  }

  public final boolean unrestricted() {
    boolean ret =
        _dscps.isEmpty()
            && _notDscps.isEmpty()
            && _dstIps.isEmpty()
            && _notDstIps.isEmpty()
            && _dstPorts.isEmpty()
            && _notDstPorts.isEmpty()
            && _ecns.isEmpty()
            && _notEcns.isEmpty()
            && _fragmentOffsets.isEmpty()
            && _notFragmentOffsets.isEmpty()
            && _icmpCodes.isEmpty()
            && _notIcmpCodes.isEmpty()
            && _icmpTypes.isEmpty()
            && _notIcmpTypes.isEmpty()
            && _ipProtocols.isEmpty()
            && _notIpProtocols.isEmpty()
            && _srcIps.isEmpty()
            && _notSrcIps.isEmpty()
            && _srcOrDstIps.isEmpty()
            && _srcOrDstPorts.isEmpty()
            && _srcPorts.isEmpty()
            && _notSrcPorts.isEmpty()
            && _states.isEmpty()
            && _tcpFlags.isEmpty();
    return ret;
  }
}

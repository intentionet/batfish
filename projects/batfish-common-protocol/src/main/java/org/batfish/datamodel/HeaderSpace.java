package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class HeaderSpace implements Serializable {

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

  private static boolean wildcardsContain(Collection<IpWildcard> wildcards, Ip ip) {
    for (IpWildcard wildcard : wildcards) {
      if (wildcard.contains(ip)) {
        return true;
      }
    }
    return false;
  }

  private SortedSet<Integer> _dscps;

  private SortedSet<IpWildcard> _dstIps;

  private SortedSet<SubRange> _dstPorts;

  private SortedSet<Protocol> _dstProtocols;

  private SortedSet<Integer> _ecns;

  private SortedSet<SubRange> _fragmentOffsets;

  private SortedSet<SubRange> _icmpCodes;

  private SortedSet<SubRange> _icmpTypes;

  private SortedSet<IpProtocol> _ipProtocols;

  private boolean _negate;

  private SortedSet<Integer> _notDscps;

  private SortedSet<IpWildcard> _notDstIps;

  private SortedSet<SubRange> _notDstPorts;

  private SortedSet<Protocol> _notDstProtocols;

  private SortedSet<Integer> _notEcns;

  private SortedSet<SubRange> _notFragmentOffsets;

  private SortedSet<SubRange> _notIcmpCodes;

  private SortedSet<SubRange> _notIcmpTypes;

  private SortedSet<IpProtocol> _notIpProtocols;

  private SortedSet<SubRange> _notPacketLengths;

  private SortedSet<IpWildcard> _notSrcIps;

  private SortedSet<SubRange> _notSrcPorts;

  private SortedSet<Protocol> _notSrcProtocols;

  private SortedSet<SubRange> _packetLengths;

  private SortedSet<IpWildcard> _srcIps;

  private SortedSet<IpWildcard> _srcOrDstIps;

  private SortedSet<SubRange> _srcOrDstPorts;

  private SortedSet<Protocol> _srcOrDstProtocols;

  private SortedSet<SubRange> _srcPorts;

  private SortedSet<Protocol> _srcProtocols;

  private SortedSet<State> _states;

  private List<TcpFlags> _tcpFlags;

  public HeaderSpace() {
    _dscps = new TreeSet<>();
    _dstIps = new TreeSet<>();
    _dstPorts = new TreeSet<>();
    _dstProtocols = new TreeSet<>();
    _ecns = new TreeSet<>();
    _fragmentOffsets = new TreeSet<>();
    _ipProtocols = new TreeSet<>();
    _packetLengths = new TreeSet<>();
    _srcIps = new TreeSet<>();
    _srcOrDstIps = new TreeSet<>();
    _srcOrDstPorts = new TreeSet<>();
    _srcOrDstProtocols = new TreeSet<>();
    _srcPorts = new TreeSet<>();
    _srcProtocols = new TreeSet<>();
    _icmpTypes = new TreeSet<>();
    _icmpCodes = new TreeSet<>();
    _states = new TreeSet<>();
    _tcpFlags = new ArrayList<>();
    _notDscps = new TreeSet<>();
    _notDstIps = new TreeSet<>();
    _notDstPorts = new TreeSet<>();
    _notDstProtocols = new TreeSet<>();
    _notEcns = new TreeSet<>();
    _notFragmentOffsets = new TreeSet<>();
    _notIcmpCodes = new TreeSet<>();
    _notIcmpTypes = new TreeSet<>();
    _notIpProtocols = new TreeSet<>();
    _notPacketLengths = new TreeSet<>();
    _notSrcIps = new TreeSet<>();
    _notSrcPorts = new TreeSet<>();
    _notSrcProtocols = new TreeSet<>();
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof HeaderSpace)) {
      return false;
    }
    HeaderSpace other = (HeaderSpace) o;
    if (!_dscps.equals(other._dscps)) {
      return false;
    }
    if (!_dstIps.equals(other._dstIps)) {
      return false;
    }
    if (!_dstPorts.equals(other._dstPorts)) {
      return false;
    }
    if (!_dstProtocols.equals(other._dstProtocols)) {
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
    if (!_notDstProtocols.equals(other._notDstProtocols)) {
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
    if (!_notPacketLengths.equals(other._notPacketLengths)) {
      return false;
    }
    if (!_notSrcIps.equals(other._notSrcIps)) {
      return false;
    }
    if (!_notSrcPorts.equals(other._notSrcPorts)) {
      return false;
    }
    if (!_notSrcProtocols.equals(other._notSrcProtocols)) {
      return false;
    }
    if (!_packetLengths.equals(other._packetLengths)) {
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
    if (!_srcOrDstProtocols.equals(other._srcOrDstProtocols)) {
      return false;
    }
    if (!_srcPorts.equals(other._srcPorts)) {
      return false;
    }
    if (!_srcProtocols.equals(other._srcProtocols)) {
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

  @JsonPropertyDescription("A set of acceptable DSCP values for a packet")
  public SortedSet<Integer> getDscps() {
    return _dscps;
  }

  @JsonPropertyDescription("A space of acceptable destination IP addresses for a packet")
  public SortedSet<IpWildcard> getDstIps() {
    return _dstIps;
  }

  @JsonPropertyDescription("A set of acceptable destination port ranges for a TCP/UDP packet")
  public SortedSet<SubRange> getDstPorts() {
    return _dstPorts;
  }

  public SortedSet<Protocol> getDstProtocols() {
    return _dstProtocols;
  }

  @JsonPropertyDescription("A set of acceptable ECN values for a packet")
  public SortedSet<Integer> getEcns() {
    return _ecns;
  }

  @JsonPropertyDescription("A set of acceptable fragment offsets for a UDP packet")
  public SortedSet<SubRange> getFragmentOffsets() {
    return _fragmentOffsets;
  }

  @JsonPropertyDescription("A set of acceptable ICMP code ranges for an ICMP packet")
  public SortedSet<SubRange> getIcmpCodes() {
    return _icmpCodes;
  }

  @JsonPropertyDescription("A set of acceptable ICMP type ranges for an ICMP packet")
  public SortedSet<SubRange> getIcmpTypes() {
    return _icmpTypes;
  }

  @JsonPropertyDescription("A set of acceptable IP protocols for a packet")
  public SortedSet<IpProtocol> getIpProtocols() {
    return _ipProtocols;
  }

  @JsonPropertyDescription(
      "Determines whether to match the complement of the stated criteria of this header space")
  public boolean getNegate() {
    return _negate;
  }

  @JsonPropertyDescription("A set of unacceptable DSCP values for a packet")
  public SortedSet<Integer> getNotDscps() {
    return _notDscps;
  }

  @JsonPropertyDescription("A space of unacceptable destination IP addresses for a packet")
  public SortedSet<IpWildcard> getNotDstIps() {
    return _notDstIps;
  }

  @JsonPropertyDescription("A set of unacceptable destination port ranges for a TCP/UDP packet")
  public SortedSet<SubRange> getNotDstPorts() {
    return _notDstPorts;
  }

  public SortedSet<Protocol> getNotDstProtocols() {
    return _notDstProtocols;
  }

  @JsonPropertyDescription("A set of unacceptable ECN values for a packet")
  public SortedSet<Integer> getNotEcns() {
    return _notEcns;
  }

  @JsonPropertyDescription("A set of unacceptable fragment offsets for a UDP packet")
  public SortedSet<SubRange> getNotFragmentOffsets() {
    return _notFragmentOffsets;
  }

  @JsonPropertyDescription("A set of unacceptable ICMP code ranges for an ICMP packet")
  public SortedSet<SubRange> getNotIcmpCodes() {
    return _notIcmpCodes;
  }

  @JsonPropertyDescription("A set of unacceptable ICMP type ranges for an ICMP packet")
  public SortedSet<SubRange> getNotIcmpTypes() {
    return _notIcmpTypes;
  }

  @JsonPropertyDescription("A set of unacceptable IP protocols for a packet")
  public SortedSet<IpProtocol> getNotIpProtocols() {
    return _notIpProtocols;
  }

  public SortedSet<SubRange> getNotPacketLengths() {
    return _notPacketLengths;
  }

  @JsonPropertyDescription("A space of unacceptable source IP addresses for a packet")
  public SortedSet<IpWildcard> getNotSrcIps() {
    return _notSrcIps;
  }

  @JsonPropertyDescription("A set of unacceptable source port ranges for a TCP/UDP packet")
  public SortedSet<SubRange> getNotSrcPorts() {
    return _notSrcPorts;
  }

  public SortedSet<Protocol> getNotSrcProtocols() {
    return _notSrcProtocols;
  }

  public SortedSet<SubRange> getPacketLengths() {
    return _packetLengths;
  }

  @JsonPropertyDescription("A space of acceptable source IP addresses for a packet")
  public SortedSet<IpWildcard> getSrcIps() {
    return _srcIps;
  }

  @JsonPropertyDescription(
      "A space of IP addresses within which either the source or the destination IP of a packet"
          + " must fall for acceptance")
  public SortedSet<IpWildcard> getSrcOrDstIps() {
    return _srcOrDstIps;
  }

  @JsonPropertyDescription(
      "A set of ranges within which either the source or the destination port of a TCP/UDP packet"
          + " must fall for acceptance")
  public SortedSet<SubRange> getSrcOrDstPorts() {
    return _srcOrDstPorts;
  }

  public SortedSet<Protocol> getSrcOrDstProtocols() {
    return _srcOrDstProtocols;
  }

  @JsonPropertyDescription("A set of acceptable source port ranges for a TCP/UDP packet")
  public SortedSet<SubRange> getSrcPorts() {
    return _srcPorts;
  }

  public SortedSet<Protocol> getSrcProtocols() {
    return _srcProtocols;
  }

  @JsonPropertyDescription("A set of acceptable abstract firewall states for a packet to match")
  public SortedSet<State> getStates() {
    return _states;
  }

  @JsonPropertyDescription("A set of acceptable TCP flag bitmasks for a TCP packet to match")
  public List<TcpFlags> getTcpFlags() {
    return _tcpFlags;
  }

  @Override
  public int hashCode() {
    // TODO: implement better hashcode
    return 0;
  }

  public boolean matches(Flow flow) {
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
    if (!_dstProtocols.isEmpty()) {
      boolean match = false;
      for (Protocol dstProtocol : _dstProtocols) {
        if (dstProtocol.getIpProtocol().equals(flow.getIpProtocol())) {
          match = true;
          Integer dstPort = dstProtocol.getPort();
          if (dstPort != null && !dstPort.equals(flow.getDstPort())) {
            match = false;
          }
          if (match) {
            break;
          }
        }
      }
      if (!match) {
        return false;
      }
    }
    if (!_notDstProtocols.isEmpty()) {
      boolean match = false;
      for (Protocol notDstProtocol : _notDstProtocols) {
        if (notDstProtocol.getIpProtocol().equals(flow.getIpProtocol())) {
          match = true;
          Integer dstPort = notDstProtocol.getPort();
          if (dstPort != null && !dstPort.equals(flow.getDstPort())) {
            match = false;
          }
          if (match) {
            return false;
          }
        }
      }
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
    if (!_packetLengths.isEmpty() && !rangesContain(_packetLengths, flow.getPacketLength())) {
      return false;
    }
    if (!_notPacketLengths.isEmpty() && rangesContain(_notPacketLengths, flow.getPacketLength())) {
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
    if (!_srcOrDstProtocols.isEmpty()) {
      boolean match = false;
      for (Protocol protocol : _srcOrDstProtocols) {
        if (protocol.getIpProtocol().equals(flow.getIpProtocol())) {
          match = true;
          Integer port = protocol.getPort();
          if (port != null && !port.equals(flow.getDstPort()) && !port.equals(flow.getSrcPort())) {
            match = false;
          }
          if (match) {
            break;
          }
        }
      }
      if (!match) {
        return false;
      }
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
    if (!_srcProtocols.isEmpty()) {
      boolean match = false;
      for (Protocol srcProtocol : _srcProtocols) {
        if (srcProtocol.getIpProtocol().equals(flow.getIpProtocol())) {
          match = true;
          Integer srcPort = srcProtocol.getPort();
          if (srcPort != null && !srcPort.equals(flow.getSrcPort())) {
            match = false;
          }
          if (match) {
            break;
          }
        }
      }
      if (!match) {
        return false;
      }
    }
    if (!_notSrcProtocols.isEmpty()) {
      boolean match = false;
      for (Protocol notSrcProtocol : _notSrcProtocols) {
        if (notSrcProtocol.getIpProtocol().equals(flow.getIpProtocol())) {
          match = true;
          Integer srcPort = notSrcProtocol.getPort();
          if (srcPort != null && !srcPort.equals(flow.getSrcPort())) {
            match = false;
          }
          if (match) {
            return false;
          }
        }
      }
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

  public void setDstIps(SortedSet<IpWildcard> dstIps) {
    _dstIps = dstIps;
  }

  public void setDstPorts(SortedSet<SubRange> dstPorts) {
    _dstPorts = dstPorts;
  }

  public void setDstProtocols(SortedSet<Protocol> dstProtocols) {
    _dstProtocols = dstProtocols;
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

  public void setIpProtocols(SortedSet<IpProtocol> ipProtocols) {
    _ipProtocols = ipProtocols;
  }

  public void setNegate(boolean negate) {
    _negate = negate;
  }

  public void setNotDscps(SortedSet<Integer> notDscps) {
    _notDscps = notDscps;
  }

  public void setNotDstIps(SortedSet<IpWildcard> notDstIps) {
    _notDstIps = notDstIps;
  }

  public void setNotDstPorts(SortedSet<SubRange> notDstPorts) {
    _notDstPorts = notDstPorts;
  }

  public void setNotDstProtocols(SortedSet<Protocol> notDstProtocols) {
    _notDstProtocols = notDstProtocols;
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

  public void setNotIpProtocols(SortedSet<IpProtocol> notIpProtocols) {
    _notIpProtocols = notIpProtocols;
  }

  public void setNotPacketLengths(SortedSet<SubRange> notPacketLengths) {
    _notPacketLengths = notPacketLengths;
  }

  public void setNotSrcIps(SortedSet<IpWildcard> notSrcIps) {
    _notSrcIps = notSrcIps;
  }

  public void setNotSrcPorts(SortedSet<SubRange> notSrcPorts) {
    _notSrcPorts = notSrcPorts;
  }

  public void setNotSrcProtocols(SortedSet<Protocol> notSrcProtocols) {
    _notSrcProtocols = notSrcProtocols;
  }

  public void setPacketLengths(SortedSet<SubRange> packetLengths) {
    _packetLengths = packetLengths;
  }

  public void setSrcIps(SortedSet<IpWildcard> srcIps) {
    _srcIps = srcIps;
  }

  public void setSrcOrDstIps(SortedSet<IpWildcard> srcOrDstIps) {
    _srcOrDstIps = srcOrDstIps;
  }

  public void setSrcOrDstPorts(SortedSet<SubRange> srcOrDstPorts) {
    _srcOrDstPorts = srcOrDstPorts;
  }

  public void setSrcOrDstProtocols(SortedSet<Protocol> srcOrDstProtocols) {
    _srcOrDstProtocols = srcOrDstProtocols;
  }

  public void setSrcPorts(SortedSet<SubRange> srcPorts) {
    _srcPorts = srcPorts;
  }

  public void setSrcProtocols(SortedSet<Protocol> srcProtocols) {
    _srcProtocols = srcProtocols;
  }

  public void setStates(SortedSet<State> states) {
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
        + ", SrcProtocols:"
        + _srcProtocols
        + ", NotSrcProtocols:"
        + _notSrcProtocols
        + ", DstProtocols:"
        + _dstProtocols
        + ", NotDstProtocols:"
        + _notDstProtocols
        + ", SrcOrDstProtocols:"
        + _srcOrDstProtocols
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
        + ", PacketLengths:"
        + _packetLengths
        + ", NotPacketLengths:"
        + _notPacketLengths
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
            && _dstProtocols.isEmpty()
            && _notDstProtocols.isEmpty()
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
            && _packetLengths.isEmpty()
            && _notPacketLengths.isEmpty()
            && _srcIps.isEmpty()
            && _notSrcIps.isEmpty()
            && _srcOrDstIps.isEmpty()
            && _srcOrDstPorts.isEmpty()
            && _srcOrDstProtocols.isEmpty()
            && _srcPorts.isEmpty()
            && _notSrcPorts.isEmpty()
            && _srcProtocols.isEmpty()
            && _notSrcProtocols.isEmpty()
            && _states.isEmpty()
            && _tcpFlags.isEmpty();
    return ret;
  }
}

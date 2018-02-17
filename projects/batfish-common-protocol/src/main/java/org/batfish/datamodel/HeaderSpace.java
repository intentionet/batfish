package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

public class HeaderSpace implements Serializable {

  public abstract static class Builder<S extends Builder<S, T>, T extends HeaderSpace> {

    protected SortedSet<Integer> _dscps;

    protected SortedSet<IpWildcard> _dstIps;

    protected SortedSet<SubRange> _dstPorts;

    protected SortedSet<Protocol> _dstProtocols;

    protected SortedSet<Integer> _ecns;

    protected SortedSet<SubRange> _fragmentOffsets;

    protected SortedSet<SubRange> _icmpCodes;

    protected SortedSet<SubRange> _icmpTypes;

    protected SortedSet<IpProtocol> _ipProtocols;

    protected boolean _negate;

    protected SortedSet<Integer> _notDscps;

    protected SortedSet<IpWildcard> _notDstIps;

    protected SortedSet<SubRange> _notDstPorts;

    protected SortedSet<Protocol> _notDstProtocols;

    protected SortedSet<Integer> _notEcns;

    protected SortedSet<SubRange> _notFragmentOffsets;

    protected SortedSet<SubRange> _notIcmpCodes;

    protected SortedSet<SubRange> _notIcmpTypes;

    protected SortedSet<IpProtocol> _notIpProtocols;

    protected SortedSet<SubRange> _notPacketLengths;

    protected SortedSet<IpWildcard> _notSrcIps;

    protected SortedSet<SubRange> _notSrcPorts;

    protected SortedSet<Protocol> _notSrcProtocols;

    protected SortedSet<SubRange> _packetLengths;

    protected SortedSet<IpWildcard> _srcIps;

    protected SortedSet<IpWildcard> _srcOrDstIps;

    protected SortedSet<SubRange> _srcOrDstPorts;

    protected SortedSet<Protocol> _srcOrDstProtocols;

    protected SortedSet<SubRange> _srcPorts;

    protected SortedSet<Protocol> _srcProtocols;

    protected SortedSet<State> _states;

    protected List<TcpFlags> _tcpFlags;

    protected Builder() {
      _dscps = Collections.emptySortedSet();
      _dstIps = Collections.emptySortedSet();
      _dstPorts = Collections.emptySortedSet();
      _dstProtocols = Collections.emptySortedSet();
      _ecns = Collections.emptySortedSet();
      _fragmentOffsets = Collections.emptySortedSet();
      _icmpCodes = Collections.emptySortedSet();
      _icmpTypes = Collections.emptySortedSet();
      _ipProtocols = Collections.emptySortedSet();
      _packetLengths = Collections.emptySortedSet();
      _srcIps = Collections.emptySortedSet();
      _srcOrDstIps = Collections.emptySortedSet();
      _srcOrDstPorts = Collections.emptySortedSet();
      _srcOrDstProtocols = Collections.emptySortedSet();
      _srcPorts = Collections.emptySortedSet();
      _srcProtocols = Collections.emptySortedSet();
      _icmpTypes = Collections.emptySortedSet();
      _icmpCodes = Collections.emptySortedSet();
      _states = Collections.emptySortedSet();
      _tcpFlags = Collections.emptyList();
      _notDscps = Collections.emptySortedSet();
      _notDstIps = Collections.emptySortedSet();
      _notDstPorts = Collections.emptySortedSet();
      _notDstProtocols = Collections.emptySortedSet();
      _notEcns = Collections.emptySortedSet();
      _notFragmentOffsets = Collections.emptySortedSet();
      _notIcmpCodes = Collections.emptySortedSet();
      _notIcmpTypes = Collections.emptySortedSet();
      _notIpProtocols = Collections.emptySortedSet();
      _notPacketLengths = Collections.emptySortedSet();
      _notSrcIps = Collections.emptySortedSet();
      _notSrcPorts = Collections.emptySortedSet();
      _notSrcProtocols = Collections.emptySortedSet();
    }

    public S setDscps(Iterable<Integer> dscps) {
      _dscps = ImmutableSortedSet.copyOf(dscps);
      return getThis();
    }

    public S setDstIps(Iterable<IpWildcard> dstIps) {
      _dstIps = ImmutableSortedSet.copyOf(dstIps);
      return getThis();
    }

    public S setDstPorts(Iterable<SubRange> dstPorts) {
      _dstPorts = ImmutableSortedSet.copyOf(dstPorts);
      return getThis();
    }

    public S setDstProtocols(Iterable<Protocol> dstProtocols) {
      _dstProtocols = ImmutableSortedSet.copyOf(dstProtocols);
      return getThis();
    }

    public S setEcns(Iterable<Integer> ecns) {
      _ecns = ImmutableSortedSet.copyOf(ecns);
      return getThis();
    }

    public S setFragmentOffsets(Iterable<SubRange> fragmentOffsets) {
      _fragmentOffsets = ImmutableSortedSet.copyOf(fragmentOffsets);
      return getThis();
    }

    public S setIcmpCodes(Iterable<SubRange> icmpCodes) {
      _icmpCodes = ImmutableSortedSet.copyOf(icmpCodes);
      return getThis();
    }

    public S setIcmpTypes(Iterable<SubRange> icmpTypes) {
      _icmpTypes = ImmutableSortedSet.copyOf(icmpTypes);
      return getThis();
    }

    public S setIpProtocols(Iterable<IpProtocol> ipProtocols) {
      _ipProtocols = ImmutableSortedSet.copyOf(ipProtocols);
      return getThis();
    }

    public S setNegate(boolean negate) {
      _negate = negate;
      return getThis();
    }

    public S setNotDscps(Iterable<Integer> notDscps) {
      _notDscps = ImmutableSortedSet.copyOf(notDscps);
      return getThis();
    }

    public S setNotDstIps(Iterable<IpWildcard> notDstIps) {
      _notDstIps = ImmutableSortedSet.copyOf(notDstIps);
      return getThis();
    }

    public S setNotDstPorts(Iterable<SubRange> notDstPorts) {
      _notDstPorts = ImmutableSortedSet.copyOf(notDstPorts);
      return getThis();
    }

    public S setNotDstProtocols(Iterable<Protocol> notDstProtocols) {
      _notDstProtocols = ImmutableSortedSet.copyOf(notDstProtocols);
      return getThis();
    }

    public S setNotEcns(Iterable<Integer> notEcns) {
      _notEcns = ImmutableSortedSet.copyOf(notEcns);
      return getThis();
    }

    public S setNotFragmentOffsets(Iterable<SubRange> notFragmentOffsets) {
      _notFragmentOffsets = ImmutableSortedSet.copyOf(notFragmentOffsets);
      return getThis();
    }

    public S setNotIcmpCodes(Iterable<SubRange> notIcmpCodes) {
      _notIcmpCodes = ImmutableSortedSet.copyOf(notIcmpCodes);
      return getThis();
    }

    public S setNotIcmpTypes(Iterable<SubRange> notIcmpTypes) {
      _notIcmpTypes = ImmutableSortedSet.copyOf(notIcmpTypes);
      return getThis();
    }

    public S setNotIpProtocols(Iterable<IpProtocol> notIpProtocols) {
      _notIpProtocols = ImmutableSortedSet.copyOf(notIpProtocols);
      return getThis();
    }

    public S setNotPacketLengths(Iterable<SubRange> notPacketLengths) {
      _notPacketLengths = ImmutableSortedSet.copyOf(notPacketLengths);
      return getThis();
    }

    public S setNotSrcIps(Iterable<IpWildcard> notSrcIps) {
      _notSrcIps = ImmutableSortedSet.copyOf(notSrcIps);
      return getThis();
    }

    public S setNotSrcPorts(Iterable<SubRange> notSrcPorts) {
      _notSrcPorts = ImmutableSortedSet.copyOf(notSrcPorts);
      return getThis();
    }

    public S setNotSrcProtocols(Iterable<Protocol> notSrcProtocols) {
      _notSrcProtocols = ImmutableSortedSet.copyOf(notSrcProtocols);
      return getThis();
    }

    public S setPacketLengths(Iterable<SubRange> packetLengths) {
      _packetLengths = ImmutableSortedSet.copyOf(packetLengths);
      return getThis();
    }

    public S setSrcIps(Iterable<IpWildcard> srcIps) {
      _srcIps = ImmutableSortedSet.copyOf(srcIps);
      return getThis();
    }

    public S setSrcOrDstIps(Iterable<IpWildcard> srcOrDstIps) {
      _srcOrDstIps = ImmutableSortedSet.copyOf(srcOrDstIps);
      return getThis();
    }

    public S setSrcOrDstPorts(Iterable<SubRange> srcOrDstPorts) {
      _srcOrDstPorts = ImmutableSortedSet.copyOf(srcOrDstPorts);
      return getThis();
    }

    public S setSrcOrDstProtocols(Iterable<Protocol> srcOrDstProtocols) {
      _srcOrDstProtocols = ImmutableSortedSet.copyOf(srcOrDstProtocols);
      return getThis();
    }

    public S setSrcPorts(Iterable<SubRange> srcPorts) {
      _srcPorts = ImmutableSortedSet.copyOf(srcPorts);
      return getThis();
    }

    public S setSrcProtocols(Iterable<Protocol> srcProtocols) {
      _srcProtocols = ImmutableSortedSet.copyOf(srcProtocols);
      return getThis();
    }

    public S setStates(Iterable<State> states) {
      _states = ImmutableSortedSet.copyOf(states);
      return getThis();
    }

    public S setTcpFlags(Iterable<TcpFlags> tcpFlags) {
      _tcpFlags = ImmutableList.copyOf(tcpFlags);
      return getThis();
    }

    public abstract T build();

    protected abstract S getThis();
  }

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
    _dscps = Collections.emptySortedSet();
    _dstIps = Collections.emptySortedSet();
    _dstPorts = Collections.emptySortedSet();
    _dstProtocols = Collections.emptySortedSet();
    _ecns = Collections.emptySortedSet();
    _fragmentOffsets = Collections.emptySortedSet();
    _icmpCodes = Collections.emptySortedSet();
    _icmpTypes = Collections.emptySortedSet();
    _ipProtocols = Collections.emptySortedSet();
    _packetLengths = Collections.emptySortedSet();
    _srcIps = Collections.emptySortedSet();
    _srcOrDstIps = Collections.emptySortedSet();
    _srcOrDstPorts = Collections.emptySortedSet();
    _srcOrDstProtocols = Collections.emptySortedSet();
    _srcPorts = Collections.emptySortedSet();
    _srcProtocols = Collections.emptySortedSet();
    _icmpTypes = Collections.emptySortedSet();
    _icmpCodes = Collections.emptySortedSet();
    _states = Collections.emptySortedSet();
    _tcpFlags = Collections.emptyList();
    _notDscps = Collections.emptySortedSet();
    _notDstIps = Collections.emptySortedSet();
    _notDstPorts = Collections.emptySortedSet();
    _notDstProtocols = Collections.emptySortedSet();
    _notEcns = Collections.emptySortedSet();
    _notFragmentOffsets = Collections.emptySortedSet();
    _notIcmpCodes = Collections.emptySortedSet();
    _notIcmpTypes = Collections.emptySortedSet();
    _notIpProtocols = Collections.emptySortedSet();
    _notPacketLengths = Collections.emptySortedSet();
    _notSrcIps = Collections.emptySortedSet();
    _notSrcPorts = Collections.emptySortedSet();
    _notSrcProtocols = Collections.emptySortedSet();
  }

  protected HeaderSpace(
      SortedSet<Integer> dscps,
      SortedSet<IpWildcard> dstIps,
      SortedSet<SubRange> dstPorts,
      SortedSet<Protocol> dstProtocols,
      SortedSet<Integer> ecns,
      SortedSet<SubRange> fragmentOffsets,
      SortedSet<SubRange> icmpCodes,
      SortedSet<SubRange> icmpTypes,
      SortedSet<IpProtocol> ipProtocols,
      boolean negate,
      SortedSet<Integer> notDscps,
      SortedSet<IpWildcard> notDstIps,
      SortedSet<SubRange> notDstPorts,
      SortedSet<Protocol> notDstProtocols,
      SortedSet<Integer> notEcns,
      SortedSet<SubRange> notFragmentOffsets,
      SortedSet<SubRange> notIcmpCodes,
      SortedSet<SubRange> notIcmpTypes,
      SortedSet<IpProtocol> notIpProtocols,
      SortedSet<SubRange> notPacketLengths,
      SortedSet<IpWildcard> notSrcIps,
      SortedSet<SubRange> notSrcPorts,
      SortedSet<Protocol> notSrcProtocols,
      SortedSet<SubRange> packetLengths,
      SortedSet<IpWildcard> srcIps,
      SortedSet<IpWildcard> srcOrDstIps,
      SortedSet<SubRange> srcOrDstPorts,
      SortedSet<Protocol> srcOrDstProtocols,
      SortedSet<SubRange> srcPorts,
      SortedSet<Protocol> srcProtocols,
      SortedSet<State> states,
      List<TcpFlags> tcpFlags) {
    _dscps = ImmutableSortedSet.copyOf(dscps);
    _dstIps = ImmutableSortedSet.copyOf(dstIps);
    _dstPorts = ImmutableSortedSet.copyOf(dstPorts);
    _dstProtocols = ImmutableSortedSet.copyOf(dstProtocols);
    _ecns = ImmutableSortedSet.copyOf(ecns);
    _fragmentOffsets = ImmutableSortedSet.copyOf(fragmentOffsets);
    _ipProtocols = ImmutableSortedSet.copyOf(ipProtocols);
    _negate = negate;
    _notDscps = ImmutableSortedSet.copyOf(notDscps);
    _notDstIps = ImmutableSortedSet.copyOf(notDstIps);
    _notDstPorts = ImmutableSortedSet.copyOf(notDstPorts);
    _notDstProtocols = ImmutableSortedSet.copyOf(notDstProtocols);
    _notEcns = ImmutableSortedSet.copyOf(notEcns);
    _notFragmentOffsets = ImmutableSortedSet.copyOf(notFragmentOffsets);
    _notIcmpCodes = ImmutableSortedSet.copyOf(notIcmpCodes);
    _notIcmpTypes = ImmutableSortedSet.copyOf(notIcmpTypes);
    _notIpProtocols = ImmutableSortedSet.copyOf(notIpProtocols);
    _notPacketLengths = ImmutableSortedSet.copyOf(notPacketLengths);
    _notSrcIps = ImmutableSortedSet.copyOf(notSrcIps);
    _notSrcPorts = ImmutableSortedSet.copyOf(notSrcPorts);
    _notSrcProtocols = ImmutableSortedSet.copyOf(notSrcProtocols);
    _packetLengths = ImmutableSortedSet.copyOf(packetLengths);
    _srcIps = ImmutableSortedSet.copyOf(srcIps);
    _srcOrDstIps = ImmutableSortedSet.copyOf(srcOrDstIps);
    _srcOrDstPorts = ImmutableSortedSet.copyOf(srcOrDstPorts);
    _srcOrDstProtocols = ImmutableSortedSet.copyOf(srcOrDstProtocols);
    _srcPorts = ImmutableSortedSet.copyOf(srcPorts);
    _srcProtocols = ImmutableSortedSet.copyOf(srcProtocols);
    _icmpTypes = ImmutableSortedSet.copyOf(icmpTypes);
    _icmpCodes = ImmutableSortedSet.copyOf(icmpCodes);
    _states = ImmutableSortedSet.copyOf(states);
    _tcpFlags = ImmutableList.copyOf(tcpFlags);
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

  public void setDscps(Iterable<Integer> dscps) {
    _dscps = ImmutableSortedSet.copyOf(dscps);
  }

  public void setDstIps(Iterable<IpWildcard> dstIps) {
    _dstIps = ImmutableSortedSet.copyOf(dstIps);
  }

  public void setDstPorts(Iterable<SubRange> dstPorts) {
    _dstPorts = ImmutableSortedSet.copyOf(dstPorts);
  }

  public void setDstProtocols(Iterable<Protocol> dstProtocols) {
    _dstProtocols = ImmutableSortedSet.copyOf(dstProtocols);
  }

  public void setEcns(Iterable<Integer> ecns) {
    _ecns = ImmutableSortedSet.copyOf(ecns);
  }

  public void setFragmentOffsets(Iterable<SubRange> fragmentOffsets) {
    _fragmentOffsets = ImmutableSortedSet.copyOf(fragmentOffsets);
  }

  public void setIcmpCodes(Iterable<SubRange> icmpCodes) {
    _icmpCodes = ImmutableSortedSet.copyOf(icmpCodes);
  }

  public void setIcmpTypes(Iterable<SubRange> icmpTypes) {
    _icmpTypes = ImmutableSortedSet.copyOf(icmpTypes);
  }

  public void setIpProtocols(Iterable<IpProtocol> ipProtocols) {
    _ipProtocols = ImmutableSortedSet.copyOf(ipProtocols);
  }

  public void setNegate(boolean negate) {
    _negate = negate;
  }

  public void setNotDscps(Iterable<Integer> notDscps) {
    _notDscps = ImmutableSortedSet.copyOf(notDscps);
  }

  public void setNotDstIps(Iterable<IpWildcard> notDstIps) {
    _notDstIps = ImmutableSortedSet.copyOf(notDstIps);
  }

  public void setNotDstPorts(Iterable<SubRange> notDstPorts) {
    _notDstPorts = ImmutableSortedSet.copyOf(notDstPorts);
  }

  public void setNotDstProtocols(Iterable<Protocol> notDstProtocols) {
    _notDstProtocols = ImmutableSortedSet.copyOf(notDstProtocols);
  }

  public void setNotEcns(Iterable<Integer> notEcns) {
    _notEcns = ImmutableSortedSet.copyOf(notEcns);
  }

  public void setNotFragmentOffsets(Iterable<SubRange> notFragmentOffsets) {
    _notFragmentOffsets = ImmutableSortedSet.copyOf(notFragmentOffsets);
  }

  public void setNotIcmpCodes(Iterable<SubRange> notIcmpCodes) {
    _notIcmpCodes = ImmutableSortedSet.copyOf(notIcmpCodes);
  }

  public void setNotIcmpTypes(Iterable<SubRange> notIcmpTypes) {
    _notIcmpTypes = ImmutableSortedSet.copyOf(notIcmpTypes);
  }

  public void setNotIpProtocols(Iterable<IpProtocol> notIpProtocols) {
    _notIpProtocols = ImmutableSortedSet.copyOf(notIpProtocols);
  }

  public void setNotPacketLengths(Iterable<SubRange> notPacketLengths) {
    _notPacketLengths = ImmutableSortedSet.copyOf(notPacketLengths);
  }

  public void setNotSrcIps(Iterable<IpWildcard> notSrcIps) {
    _notSrcIps = ImmutableSortedSet.copyOf(notSrcIps);
  }

  public void setNotSrcPorts(Iterable<SubRange> notSrcPorts) {
    _notSrcPorts = ImmutableSortedSet.copyOf(notSrcPorts);
  }

  public void setNotSrcProtocols(Iterable<Protocol> notSrcProtocols) {
    _notSrcProtocols = ImmutableSortedSet.copyOf(notSrcProtocols);
  }

  public void setPacketLengths(Iterable<SubRange> packetLengths) {
    _packetLengths = ImmutableSortedSet.copyOf(packetLengths);
  }

  public void setSrcIps(Iterable<IpWildcard> srcIps) {
    _srcIps = ImmutableSortedSet.copyOf(srcIps);
  }

  public void setSrcOrDstIps(Iterable<IpWildcard> srcOrDstIps) {
    _srcOrDstIps = ImmutableSortedSet.copyOf(srcOrDstIps);
  }

  public void setSrcOrDstPorts(Iterable<SubRange> srcOrDstPorts) {
    _srcOrDstPorts = ImmutableSortedSet.copyOf(srcOrDstPorts);
  }

  public void setSrcOrDstProtocols(Iterable<Protocol> srcOrDstProtocols) {
    _srcOrDstProtocols = ImmutableSortedSet.copyOf(srcOrDstProtocols);
  }

  public void setSrcPorts(Iterable<SubRange> srcPorts) {
    _srcPorts = ImmutableSortedSet.copyOf(srcPorts);
  }

  public void setSrcProtocols(Iterable<Protocol> srcProtocols) {
    _srcProtocols = ImmutableSortedSet.copyOf(srcProtocols);
  }

  public void setStates(Iterable<State> states) {
    _states = ImmutableSortedSet.copyOf(states);
  }

  public void setTcpFlags(Iterable<TcpFlags> tcpFlags) {
    _tcpFlags = ImmutableList.copyOf(tcpFlags);
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

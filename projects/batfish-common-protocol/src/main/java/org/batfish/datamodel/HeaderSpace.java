package org.batfish.datamodel;

import static org.batfish.common.util.CommonUtil.nullIfEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import org.batfish.common.util.CommonUtil;

public class HeaderSpace implements Serializable, Comparable<HeaderSpace> {

  public static class Builder {

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

    private Builder() {
      _dscps = ImmutableSortedSet.of();
      _dstIps = ImmutableSortedSet.of();
      _dstPorts = ImmutableSortedSet.of();
      _dstProtocols = ImmutableSortedSet.of();
      _ecns = ImmutableSortedSet.of();
      _fragmentOffsets = ImmutableSortedSet.of();
      _icmpCodes = ImmutableSortedSet.of();
      _icmpTypes = ImmutableSortedSet.of();
      _ipProtocols = ImmutableSortedSet.of();
      _packetLengths = ImmutableSortedSet.of();
      _srcIps = ImmutableSortedSet.of();
      _srcOrDstIps = ImmutableSortedSet.of();
      _srcOrDstPorts = ImmutableSortedSet.of();
      _srcOrDstProtocols = ImmutableSortedSet.of();
      _srcPorts = ImmutableSortedSet.of();
      _srcProtocols = ImmutableSortedSet.of();
      _icmpTypes = ImmutableSortedSet.of();
      _icmpCodes = ImmutableSortedSet.of();
      _states = ImmutableSortedSet.of();
      _tcpFlags = ImmutableList.of();
      _notDscps = ImmutableSortedSet.of();
      _notDstIps = ImmutableSortedSet.of();
      _notDstPorts = ImmutableSortedSet.of();
      _notDstProtocols = ImmutableSortedSet.of();
      _notEcns = ImmutableSortedSet.of();
      _notFragmentOffsets = ImmutableSortedSet.of();
      _notIcmpCodes = ImmutableSortedSet.of();
      _notIcmpTypes = ImmutableSortedSet.of();
      _notIpProtocols = ImmutableSortedSet.of();
      _notPacketLengths = ImmutableSortedSet.of();
      _notSrcIps = ImmutableSortedSet.of();
      _notSrcPorts = ImmutableSortedSet.of();
      _notSrcProtocols = ImmutableSortedSet.of();
    }

    public HeaderSpace build() {
      return new HeaderSpace(this);
    }

    public SortedSet<Integer> getDscps() {
      return _dscps;
    }

    public SortedSet<IpWildcard> getDstIps() {
      return _dstIps;
    }

    public SortedSet<SubRange> getDstPorts() {
      return _dstPorts;
    }

    public SortedSet<Protocol> getDstProtocols() {
      return _dstProtocols;
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

    public SortedSet<IpProtocol> getIpProtocols() {
      return _ipProtocols;
    }

    public boolean getNegate() {
      return _negate;
    }

    public SortedSet<Integer> getNotDscps() {
      return _notDscps;
    }

    public SortedSet<IpWildcard> getNotDstIps() {
      return _notDstIps;
    }

    public SortedSet<SubRange> getNotDstPorts() {
      return _notDstPorts;
    }

    public SortedSet<Protocol> getNotDstProtocols() {
      return _notDstProtocols;
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

    public SortedSet<IpProtocol> getNotIpProtocols() {
      return _notIpProtocols;
    }

    public SortedSet<SubRange> getNotPacketLengths() {
      return _notPacketLengths;
    }

    public SortedSet<IpWildcard> getNotSrcIps() {
      return _notSrcIps;
    }

    public SortedSet<SubRange> getNotSrcPorts() {
      return _notSrcPorts;
    }

    public SortedSet<Protocol> getNotSrcProtocols() {
      return _notSrcProtocols;
    }

    public SortedSet<SubRange> getPacketLengths() {
      return _packetLengths;
    }

    public SortedSet<IpWildcard> getSrcIps() {
      return _srcIps;
    }

    public SortedSet<IpWildcard> getSrcOrDstIps() {
      return _srcOrDstIps;
    }

    public SortedSet<SubRange> getSrcOrDstPorts() {
      return _srcOrDstPorts;
    }

    public SortedSet<Protocol> getSrcOrDstProtocols() {
      return _srcOrDstProtocols;
    }

    public SortedSet<SubRange> getSrcPorts() {
      return _srcPorts;
    }

    public SortedSet<Protocol> getSrcProtocols() {
      return _srcProtocols;
    }

    public SortedSet<State> getStates() {
      return _states;
    }

    public List<TcpFlags> getTcpFlags() {
      return _tcpFlags;
    }

    public Builder setDscps(Iterable<Integer> dscps) {
      _dscps = ImmutableSortedSet.copyOf(dscps);
      return this;
    }

    public Builder setDstIps(Iterable<IpWildcard> dstIps) {
      _dstIps = ImmutableSortedSet.copyOf(dstIps);
      return this;
    }

    public Builder setDstPorts(Iterable<SubRange> dstPorts) {
      _dstPorts = ImmutableSortedSet.copyOf(dstPorts);
      return this;
    }

    public Builder setDstProtocols(Iterable<Protocol> dstProtocols) {
      _dstProtocols = ImmutableSortedSet.copyOf(dstProtocols);
      return this;
    }

    public Builder setEcns(Iterable<Integer> ecns) {
      _ecns = ImmutableSortedSet.copyOf(ecns);
      return this;
    }

    public Builder setFragmentOffsets(Iterable<SubRange> fragmentOffsets) {
      _fragmentOffsets = ImmutableSortedSet.copyOf(fragmentOffsets);
      return this;
    }

    public Builder setIcmpCodes(Iterable<SubRange> icmpCodes) {
      _icmpCodes = ImmutableSortedSet.copyOf(icmpCodes);
      return this;
    }

    public Builder setIcmpTypes(Iterable<SubRange> icmpTypes) {
      _icmpTypes = ImmutableSortedSet.copyOf(icmpTypes);
      return this;
    }

    public Builder setIpProtocols(Iterable<IpProtocol> ipProtocols) {
      _ipProtocols = ImmutableSortedSet.copyOf(ipProtocols);
      return this;
    }

    public Builder setNegate(boolean negate) {
      _negate = negate;
      return this;
    }

    public Builder setNotDscps(Iterable<Integer> notDscps) {
      _notDscps = ImmutableSortedSet.copyOf(notDscps);
      return this;
    }

    public Builder setNotDstIps(Iterable<IpWildcard> notDstIps) {
      _notDstIps = ImmutableSortedSet.copyOf(notDstIps);
      return this;
    }

    public Builder setNotDstPorts(Iterable<SubRange> notDstPorts) {
      _notDstPorts = ImmutableSortedSet.copyOf(notDstPorts);
      return this;
    }

    public Builder setNotDstProtocols(Iterable<Protocol> notDstProtocols) {
      _notDstProtocols = ImmutableSortedSet.copyOf(notDstProtocols);
      return this;
    }

    public Builder setNotEcns(Iterable<Integer> notEcns) {
      _notEcns = ImmutableSortedSet.copyOf(notEcns);
      return this;
    }

    public Builder setNotFragmentOffsets(Iterable<SubRange> notFragmentOffsets) {
      _notFragmentOffsets = ImmutableSortedSet.copyOf(notFragmentOffsets);
      return this;
    }

    public Builder setNotIcmpCodes(Iterable<SubRange> notIcmpCodes) {
      _notIcmpCodes = ImmutableSortedSet.copyOf(notIcmpCodes);
      return this;
    }

    public Builder setNotIcmpTypes(Iterable<SubRange> notIcmpTypes) {
      _notIcmpTypes = ImmutableSortedSet.copyOf(notIcmpTypes);
      return this;
    }

    public Builder setNotIpProtocols(Iterable<IpProtocol> notIpProtocols) {
      _notIpProtocols = ImmutableSortedSet.copyOf(notIpProtocols);
      return this;
    }

    public Builder setNotPacketLengths(Iterable<SubRange> notPacketLengths) {
      _notPacketLengths = ImmutableSortedSet.copyOf(notPacketLengths);
      return this;
    }

    public Builder setNotSrcIps(Iterable<IpWildcard> notSrcIps) {
      _notSrcIps = ImmutableSortedSet.copyOf(notSrcIps);
      return this;
    }

    public Builder setNotSrcPorts(Iterable<SubRange> notSrcPorts) {
      _notSrcPorts = ImmutableSortedSet.copyOf(notSrcPorts);
      return this;
    }

    public Builder setNotSrcProtocols(Iterable<Protocol> notSrcProtocols) {
      _notSrcProtocols = ImmutableSortedSet.copyOf(notSrcProtocols);
      return this;
    }

    public Builder setPacketLengths(Iterable<SubRange> packetLengths) {
      _packetLengths = ImmutableSortedSet.copyOf(packetLengths);
      return this;
    }

    public Builder setSrcIps(Iterable<IpWildcard> srcIps) {
      _srcIps = ImmutableSortedSet.copyOf(srcIps);
      return this;
    }

    public Builder setSrcOrDstIps(Iterable<IpWildcard> srcOrDstIps) {
      _srcOrDstIps = ImmutableSortedSet.copyOf(srcOrDstIps);
      return this;
    }

    public Builder setSrcOrDstPorts(Iterable<SubRange> srcOrDstPorts) {
      _srcOrDstPorts = ImmutableSortedSet.copyOf(srcOrDstPorts);
      return this;
    }

    public Builder setSrcOrDstProtocols(Iterable<Protocol> srcOrDstProtocols) {
      _srcOrDstProtocols = ImmutableSortedSet.copyOf(srcOrDstProtocols);
      return this;
    }

    public Builder setSrcPorts(Iterable<SubRange> srcPorts) {
      _srcPorts = ImmutableSortedSet.copyOf(srcPorts);
      return this;
    }

    public Builder setSrcProtocols(Iterable<Protocol> srcProtocols) {
      _srcProtocols = ImmutableSortedSet.copyOf(srcProtocols);
      return this;
    }

    public Builder setStates(Iterable<State> states) {
      _states = ImmutableSortedSet.copyOf(states);
      return this;
    }

    public Builder setTcpFlags(Iterable<TcpFlags> tcpFlags) {
      _tcpFlags = ImmutableList.copyOf(tcpFlags);
      return this;
    }
  }

  private static final Comparator<HeaderSpace> COMPARATOR =
      Comparator.comparing(HeaderSpace::getDscps, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getDstIps, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getDstPorts, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getDstProtocols, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getEcns, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getFragmentOffsets, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getIcmpCodes, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getIcmpTypes, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getIpProtocols, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getNegate)
          .thenComparing(HeaderSpace::getNotDscps, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getNotDstIps, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getNotDstPorts, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getNotDstProtocols, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getNotEcns, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getNotFragmentOffsets, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getNotIcmpCodes, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getNotIcmpTypes, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getNotIpProtocols, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getNotPacketLengths, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getNotSrcIps, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getNotSrcPorts, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getNotSrcProtocols, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getPacketLengths, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getSrcIps, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getSrcOrDstIps, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getSrcOrDstPorts, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getSrcOrDstProtocols, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getSrcPorts, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getSrcProtocols, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getStates, CommonUtil::compareIterable)
          .thenComparing(HeaderSpace::getTcpFlags, CommonUtil::compareIterable);

  private static final String PROP_DSCPS = "dscps";

  private static final String PROP_DST_IPS = "dstIps";

  private static final String PROP_DST_PORTS = "dstPorts";

  private static final String PROP_DST_PROTOCOLS = "dstProtocols";

  private static final String PROP_ECNS = "ecns";

  private static final String PROP_FRAGMENT_OFFSETS = "fragmentOffsets";

  private static final String PROP_ICMP_CODES = "icmpCodes";

  private static final String PROP_ICMP_TYPES = "icmpTypes";

  private static final String PROP_IP_PROTOCOLS = "ipProtocols";

  private static final String PROP_NEGATE = "negate";

  private static final String PROP_NOT_DSCPS = "notDscps";

  private static final String PROP_NOT_DST_IPS = "notDstIps";

  private static final String PROP_NOT_DST_PORTS = "notDstPorts";

  private static final String PROP_NOT_DST_PROTOCOLS = "notDstProtocols";

  private static final String PROP_NOT_ECNS = "notEcns";

  private static final String PROP_NOT_FRAGMENT_OFFSETS = "notFragmentOffsets";

  private static final String PROP_NOT_ICMP_CODES = "notIcmpCodes";

  private static final String PROP_NOT_ICMP_TYPES = "notIcmpTypes";

  private static final String PROP_NOT_IP_PROTOCOLS = "notIpProtocols";

  private static final String PROP_NOT_PACKET_LENGTHS = "notPacketLengths";

  private static final String PROP_NOT_SRC_IPS = "notSrcIps";

  private static final String PROP_NOT_SRC_PORTS = "notSrcPorts";

  private static final String PROP_NOT_SRC_PROTOCOLS = "notSrcProtocols";

  private static final String PROP_PACKET_LENGTHS = "packetLengths";

  private static final String PROP_SRC_IPS = "srcIps";

  private static final String PROP_SRC_OR_DST_IPS = "srcOrDstIps";

  private static final String PROP_SRC_OR_DST_PORTS = "srcOrDstPorts";

  private static final String PROP_SRC_OR_DST_PROTOCOLS = "srcOrDstProtocols";

  private static final String PROP_SRC_PORTS = "srcPorts";

  private static final String PROP_SRC_PROTOCOLS = "srcProtocols";

  private static final String PROP_STATES = "states";

  private static final String PROP_TCP_FLAGS = "tcpFlags";

  /** */
  private static final long serialVersionUID = 1L;

  public static Builder builder() {
    return new Builder();
  }

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
      if (wildcard.containsIp(ip)) {
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

  private HeaderSpace(Builder builder) {
    _dscps = ImmutableSortedSet.copyOf(builder._dscps);
    _dstIps = ImmutableSortedSet.copyOf(builder._dstIps);
    _dstPorts = ImmutableSortedSet.copyOf(builder._dstPorts);
    _dstProtocols = ImmutableSortedSet.copyOf(builder._dstProtocols);
    _ecns = ImmutableSortedSet.copyOf(builder._ecns);
    _fragmentOffsets = ImmutableSortedSet.copyOf(builder._fragmentOffsets);
    _ipProtocols = ImmutableSortedSet.copyOf(builder._ipProtocols);
    _negate = builder._negate;
    _notDscps = ImmutableSortedSet.copyOf(builder._notDscps);
    _notDstIps = ImmutableSortedSet.copyOf(builder._notDstIps);
    _notDstPorts = ImmutableSortedSet.copyOf(builder._notDstPorts);
    _notDstProtocols = ImmutableSortedSet.copyOf(builder._notDstProtocols);
    _notEcns = ImmutableSortedSet.copyOf(builder._notEcns);
    _notFragmentOffsets = ImmutableSortedSet.copyOf(builder._notFragmentOffsets);
    _notIcmpCodes = ImmutableSortedSet.copyOf(builder._notIcmpCodes);
    _notIcmpTypes = ImmutableSortedSet.copyOf(builder._notIcmpTypes);
    _notIpProtocols = ImmutableSortedSet.copyOf(builder._notIpProtocols);
    _notPacketLengths = ImmutableSortedSet.copyOf(builder._notPacketLengths);
    _notSrcIps = ImmutableSortedSet.copyOf(builder._notSrcIps);
    _notSrcPorts = ImmutableSortedSet.copyOf(builder._notSrcPorts);
    _notSrcProtocols = ImmutableSortedSet.copyOf(builder._notSrcProtocols);
    _packetLengths = ImmutableSortedSet.copyOf(builder._packetLengths);
    _srcIps = ImmutableSortedSet.copyOf(builder._srcIps);
    _srcOrDstIps = ImmutableSortedSet.copyOf(builder._srcOrDstIps);
    _srcOrDstPorts = ImmutableSortedSet.copyOf(builder._srcOrDstPorts);
    _srcOrDstProtocols = ImmutableSortedSet.copyOf(builder._srcOrDstProtocols);
    _srcPorts = ImmutableSortedSet.copyOf(builder._srcPorts);
    _srcProtocols = ImmutableSortedSet.copyOf(builder._srcProtocols);
    _icmpTypes = ImmutableSortedSet.copyOf(builder._icmpTypes);
    _icmpCodes = ImmutableSortedSet.copyOf(builder._icmpCodes);
    _states = ImmutableSortedSet.copyOf(builder._states);
    _tcpFlags = ImmutableList.copyOf(builder._tcpFlags);
  }

  @Override
  public int compareTo(HeaderSpace o) {
    return COMPARATOR.compare(this, o);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof HeaderSpace)) {
      return false;
    }
    HeaderSpace other = (HeaderSpace) o;
    return _dscps.equals(other._dscps)
        && _dstIps.equals(other._dstIps)
        && _dstPorts.equals(other._dstPorts)
        && _dstProtocols.equals(other._dstProtocols)
        && _ecns.equals(other._ecns)
        && _fragmentOffsets.equals(other._fragmentOffsets)
        && _icmpCodes.equals(other._icmpCodes)
        && _icmpTypes.equals(other._icmpTypes)
        && _ipProtocols.equals(other._ipProtocols)
        && _negate == other._negate
        && _notDscps.equals(other._notDscps)
        && _notDstIps.equals(other._notDstIps)
        && _notDstPorts.equals(other._notDstPorts)
        && _notDstProtocols.equals(other._notDstProtocols)
        && _notEcns.equals(other._notEcns)
        && _notFragmentOffsets.equals(other._notFragmentOffsets)
        && _notIcmpCodes.equals(other._notIcmpCodes)
        && _notIcmpTypes.equals(other._notIcmpTypes)
        && _notIpProtocols.equals(other._notIpProtocols)
        && _notPacketLengths.equals(other._notPacketLengths)
        && _notSrcIps.equals(other._notSrcIps)
        && _notSrcPorts.equals(other._notSrcPorts)
        && _notSrcProtocols.equals(other._notSrcProtocols)
        && _packetLengths.equals(other._packetLengths)
        && _srcIps.equals(other._srcIps)
        && _srcOrDstIps.equals(other._srcOrDstIps)
        && _srcOrDstPorts.equals(other._srcOrDstPorts)
        && _srcOrDstProtocols.equals(other._srcOrDstProtocols)
        && _srcPorts.equals(other._srcPorts)
        && _srcProtocols.equals(other._srcProtocols)
        && _states.equals(other._states)
        && _tcpFlags.equals(other._tcpFlags);
  }

  @JsonPropertyDescription("A set of acceptable DSCP values for a packet")
  @JsonProperty(PROP_DSCPS)
  public SortedSet<Integer> getDscps() {
    return _dscps;
  }

  /** The empty set of dstIps is interpreted as no constraint, or all IPs */
  @JsonPropertyDescription("A space of acceptable destination IP addresses for a packet")
  @JsonProperty(PROP_DST_IPS)
  public SortedSet<IpWildcard> getDstIps() {
    return _dstIps;
  }

  @JsonPropertyDescription("A set of acceptable destination port ranges for a TCP/UDP packet")
  @JsonProperty(PROP_DST_PORTS)
  public SortedSet<SubRange> getDstPorts() {
    return _dstPorts;
  }

  @JsonProperty(PROP_DST_PROTOCOLS)
  public SortedSet<Protocol> getDstProtocols() {
    return _dstProtocols;
  }

  @JsonPropertyDescription("A set of acceptable ECN values for a packet")
  @JsonProperty(PROP_ECNS)
  public SortedSet<Integer> getEcns() {
    return _ecns;
  }

  @JsonPropertyDescription("A set of acceptable fragment offsets for a UDP packet")
  @JsonProperty(PROP_FRAGMENT_OFFSETS)
  public SortedSet<SubRange> getFragmentOffsets() {
    return _fragmentOffsets;
  }

  @JsonPropertyDescription("A set of acceptable ICMP code ranges for an ICMP packet")
  @JsonProperty(PROP_ICMP_CODES)
  public SortedSet<SubRange> getIcmpCodes() {
    return _icmpCodes;
  }

  @JsonPropertyDescription("A set of acceptable ICMP type ranges for an ICMP packet")
  @JsonProperty(PROP_ICMP_TYPES)
  public SortedSet<SubRange> getIcmpTypes() {
    return _icmpTypes;
  }

  @JsonPropertyDescription("A set of acceptable IP protocols for a packet")
  @JsonProperty(PROP_IP_PROTOCOLS)
  public SortedSet<IpProtocol> getIpProtocols() {
    return _ipProtocols;
  }

  @JsonPropertyDescription(
      "Determines whether to match the complement of the stated criteria of this header space")
  @JsonProperty(PROP_NEGATE)
  public boolean getNegate() {
    return _negate;
  }

  @JsonPropertyDescription("A set of unacceptable DSCP values for a packet")
  @JsonProperty(PROP_NOT_DSCPS)
  public SortedSet<Integer> getNotDscps() {
    return _notDscps;
  }

  @JsonPropertyDescription("A space of unacceptable destination IP addresses for a packet")
  @JsonProperty(PROP_NOT_DST_IPS)
  public SortedSet<IpWildcard> getNotDstIps() {
    return _notDstIps;
  }

  @JsonPropertyDescription("A set of unacceptable destination port ranges for a TCP/UDP packet")
  @JsonProperty(PROP_NOT_DST_PORTS)
  public SortedSet<SubRange> getNotDstPorts() {
    return _notDstPorts;
  }

  @JsonProperty(PROP_NOT_DST_PROTOCOLS)
  public SortedSet<Protocol> getNotDstProtocols() {
    return _notDstProtocols;
  }

  @JsonPropertyDescription("A set of unacceptable ECN values for a packet")
  @JsonProperty(PROP_NOT_ECNS)
  public SortedSet<Integer> getNotEcns() {
    return _notEcns;
  }

  @JsonPropertyDescription("A set of unacceptable fragment offsets for a UDP packet")
  @JsonProperty(PROP_NOT_FRAGMENT_OFFSETS)
  public SortedSet<SubRange> getNotFragmentOffsets() {
    return _notFragmentOffsets;
  }

  @JsonPropertyDescription("A set of unacceptable ICMP code ranges for an ICMP packet")
  @JsonProperty(PROP_NOT_ICMP_CODES)
  public SortedSet<SubRange> getNotIcmpCodes() {
    return _notIcmpCodes;
  }

  @JsonPropertyDescription("A set of unacceptable ICMP type ranges for an ICMP packet")
  @JsonProperty(PROP_NOT_ICMP_TYPES)
  public SortedSet<SubRange> getNotIcmpTypes() {
    return _notIcmpTypes;
  }

  @JsonPropertyDescription("A set of unacceptable IP protocols for a packet")
  @JsonProperty(PROP_NOT_IP_PROTOCOLS)
  public SortedSet<IpProtocol> getNotIpProtocols() {
    return _notIpProtocols;
  }

  @JsonProperty(PROP_NOT_PACKET_LENGTHS)
  public SortedSet<SubRange> getNotPacketLengths() {
    return _notPacketLengths;
  }

  @JsonPropertyDescription("A space of unacceptable source IP addresses for a packet")
  @JsonProperty(PROP_NOT_SRC_IPS)
  public SortedSet<IpWildcard> getNotSrcIps() {
    return _notSrcIps;
  }

  @JsonPropertyDescription("A set of unacceptable source port ranges for a TCP/UDP packet")
  @JsonProperty(PROP_NOT_SRC_PORTS)
  public SortedSet<SubRange> getNotSrcPorts() {
    return _notSrcPorts;
  }

  @JsonProperty(PROP_NOT_SRC_PROTOCOLS)
  public SortedSet<Protocol> getNotSrcProtocols() {
    return _notSrcProtocols;
  }

  @JsonProperty(PROP_PACKET_LENGTHS)
  public SortedSet<SubRange> getPacketLengths() {
    return _packetLengths;
  }

  @JsonPropertyDescription("A space of acceptable source IP addresses for a packet")
  @JsonProperty(PROP_SRC_IPS)
  public SortedSet<IpWildcard> getSrcIps() {
    return _srcIps;
  }

  @JsonPropertyDescription(
      "A space of IP addresses within which either the source or the destination IP of a packet"
          + " must fall for acceptance")
  @JsonProperty(PROP_SRC_OR_DST_IPS)
  public SortedSet<IpWildcard> getSrcOrDstIps() {
    return _srcOrDstIps;
  }

  @JsonPropertyDescription(
      "A set of ranges within which either the source or the destination port of a TCP/UDP packet"
          + " must fall for acceptance")
  @JsonProperty(PROP_SRC_OR_DST_PORTS)
  public SortedSet<SubRange> getSrcOrDstPorts() {
    return _srcOrDstPorts;
  }

  @JsonProperty(PROP_SRC_OR_DST_PROTOCOLS)
  public SortedSet<Protocol> getSrcOrDstProtocols() {
    return _srcOrDstProtocols;
  }

  @JsonPropertyDescription("A set of acceptable source port ranges for a TCP/UDP packet")
  @JsonProperty(PROP_SRC_PORTS)
  public SortedSet<SubRange> getSrcPorts() {
    return _srcPorts;
  }

  @JsonProperty(PROP_SRC_PROTOCOLS)
  public SortedSet<Protocol> getSrcProtocols() {
    return _srcProtocols;
  }

  @JsonPropertyDescription("A set of acceptable abstract firewall states for a packet to match")
  @JsonProperty(PROP_STATES)
  public SortedSet<State> getStates() {
    return _states;
  }

  @JsonPropertyDescription("A set of acceptable TCP flag bitmasks for a TCP packet to match")
  @JsonProperty(PROP_TCP_FLAGS)
  public List<TcpFlags> getTcpFlags() {
    return _tcpFlags;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        _dscps,
        _dstIps,
        _dstPorts,
        _dstProtocols,
        _ecns,
        _fragmentOffsets,
        _icmpCodes,
        _icmpTypes,
        _ipProtocols,
        _negate,
        _notDscps,
        _notDstIps,
        _notDstPorts,
        _notDstProtocols,
        _notEcns,
        _notFragmentOffsets,
        _notIcmpCodes,
        _notIcmpTypes,
        _notIpProtocols,
        _notPacketLengths,
        _notSrcIps,
        _notSrcPorts,
        _notSrcProtocols,
        _packetLengths,
        _srcIps,
        _srcOrDstIps,
        _srcOrDstPorts,
        _srcOrDstProtocols,
        _srcPorts,
        _srcProtocols,
        _states,
        _tcpFlags);
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

  public Builder rebuild() {
    return builder()
        .setDscps(_dscps)
        .setDstIps(_dstIps)
        .setDstPorts(_dstPorts)
        .setDstProtocols(_dstProtocols)
        .setEcns(_ecns)
        .setFragmentOffsets(_fragmentOffsets)
        .setIcmpCodes(_icmpCodes)
        .setIcmpTypes(_icmpTypes)
        .setIpProtocols(_ipProtocols)
        .setNegate(_negate)
        .setNotDscps(_notDscps)
        .setNotDstIps(_notDstIps)
        .setNotDstPorts(_notDstPorts)
        .setNotDstProtocols(_notDstProtocols)
        .setNotEcns(_notEcns)
        .setNotFragmentOffsets(_notFragmentOffsets)
        .setNotIcmpCodes(_notIcmpCodes)
        .setNotIcmpTypes(_notIcmpTypes)
        .setNotIpProtocols(_notIpProtocols)
        .setNotPacketLengths(_notPacketLengths)
        .setNotSrcIps(_notSrcIps)
        .setNotSrcPorts(_notSrcPorts)
        .setNotSrcProtocols(_notSrcProtocols)
        .setPacketLengths(_packetLengths)
        .setSrcIps(_srcIps)
        .setSrcOrDstIps(_srcOrDstIps)
        .setSrcOrDstPorts(_srcOrDstPorts)
        .setSrcOrDstProtocols(_srcOrDstProtocols)
        .setSrcPorts(_srcPorts)
        .setSrcProtocols(_srcProtocols)
        .setStates(_states)
        .setTcpFlags(_tcpFlags);
  }

  @JsonProperty(PROP_DSCPS)
  public void setDscps(Iterable<Integer> dscps) {
    _dscps = ImmutableSortedSet.copyOf(dscps);
  }

  @JsonProperty(PROP_DST_IPS)
  public void setDstIps(Iterable<IpWildcard> dstIps) {
    _dstIps = ImmutableSortedSet.copyOf(dstIps);
  }

  @JsonProperty(PROP_DST_PORTS)
  public void setDstPorts(Iterable<SubRange> dstPorts) {
    _dstPorts = ImmutableSortedSet.copyOf(dstPorts);
  }

  @JsonProperty(PROP_DST_PROTOCOLS)
  public void setDstProtocols(Iterable<Protocol> dstProtocols) {
    _dstProtocols = ImmutableSortedSet.copyOf(dstProtocols);
  }

  @JsonProperty(PROP_ECNS)
  public void setEcns(Iterable<Integer> ecns) {
    _ecns = ImmutableSortedSet.copyOf(ecns);
  }

  @JsonProperty(PROP_FRAGMENT_OFFSETS)
  public void setFragmentOffsets(Iterable<SubRange> fragmentOffsets) {
    _fragmentOffsets = ImmutableSortedSet.copyOf(fragmentOffsets);
  }

  @JsonProperty(PROP_ICMP_CODES)
  public void setIcmpCodes(Iterable<SubRange> icmpCodes) {
    _icmpCodes = ImmutableSortedSet.copyOf(icmpCodes);
  }

  @JsonProperty(PROP_ICMP_TYPES)
  public void setIcmpTypes(Iterable<SubRange> icmpTypes) {
    _icmpTypes = ImmutableSortedSet.copyOf(icmpTypes);
  }

  @JsonProperty(PROP_IP_PROTOCOLS)
  public void setIpProtocols(Iterable<IpProtocol> ipProtocols) {
    _ipProtocols = ImmutableSortedSet.copyOf(ipProtocols);
  }

  @JsonProperty(PROP_NEGATE)
  public void setNegate(boolean negate) {
    _negate = negate;
  }

  @JsonProperty(PROP_NOT_DSCPS)
  public void setNotDscps(Iterable<Integer> notDscps) {
    _notDscps = ImmutableSortedSet.copyOf(notDscps);
  }

  @JsonProperty(PROP_NOT_DST_IPS)
  public void setNotDstIps(Iterable<IpWildcard> notDstIps) {
    _notDstIps = ImmutableSortedSet.copyOf(notDstIps);
  }

  @JsonProperty(PROP_NOT_DST_PORTS)
  public void setNotDstPorts(Iterable<SubRange> notDstPorts) {
    _notDstPorts = ImmutableSortedSet.copyOf(notDstPorts);
  }

  @JsonProperty(PROP_NOT_DST_PROTOCOLS)
  public void setNotDstProtocols(Iterable<Protocol> notDstProtocols) {
    _notDstProtocols = ImmutableSortedSet.copyOf(notDstProtocols);
  }

  @JsonProperty(PROP_NOT_ECNS)
  public void setNotEcns(Iterable<Integer> notEcns) {
    _notEcns = ImmutableSortedSet.copyOf(notEcns);
  }

  @JsonProperty(PROP_NOT_FRAGMENT_OFFSETS)
  public void setNotFragmentOffsets(Iterable<SubRange> notFragmentOffsets) {
    _notFragmentOffsets = ImmutableSortedSet.copyOf(notFragmentOffsets);
  }

  @JsonProperty(PROP_NOT_ICMP_CODES)
  public void setNotIcmpCodes(Iterable<SubRange> notIcmpCodes) {
    _notIcmpCodes = ImmutableSortedSet.copyOf(notIcmpCodes);
  }

  @JsonProperty(PROP_NOT_ICMP_TYPES)
  public void setNotIcmpTypes(Iterable<SubRange> notIcmpTypes) {
    _notIcmpTypes = ImmutableSortedSet.copyOf(notIcmpTypes);
  }

  @JsonProperty(PROP_NOT_IP_PROTOCOLS)
  public void setNotIpProtocols(Iterable<IpProtocol> notIpProtocols) {
    _notIpProtocols = ImmutableSortedSet.copyOf(notIpProtocols);
  }

  @JsonProperty(PROP_NOT_PACKET_LENGTHS)
  public void setNotPacketLengths(Iterable<SubRange> notPacketLengths) {
    _notPacketLengths = ImmutableSortedSet.copyOf(notPacketLengths);
  }

  @JsonProperty(PROP_NOT_SRC_IPS)
  public void setNotSrcIps(Iterable<IpWildcard> notSrcIps) {
    _notSrcIps = ImmutableSortedSet.copyOf(notSrcIps);
  }

  @JsonProperty(PROP_NOT_SRC_PORTS)
  public void setNotSrcPorts(Iterable<SubRange> notSrcPorts) {
    _notSrcPorts = ImmutableSortedSet.copyOf(notSrcPorts);
  }

  @JsonProperty(PROP_NOT_SRC_PROTOCOLS)
  public void setNotSrcProtocols(Iterable<Protocol> notSrcProtocols) {
    _notSrcProtocols = ImmutableSortedSet.copyOf(notSrcProtocols);
  }

  @JsonProperty(PROP_PACKET_LENGTHS)
  public void setPacketLengths(Iterable<SubRange> packetLengths) {
    _packetLengths = ImmutableSortedSet.copyOf(packetLengths);
  }

  @JsonProperty(PROP_SRC_IPS)
  public void setSrcIps(Iterable<IpWildcard> srcIps) {
    _srcIps = ImmutableSortedSet.copyOf(srcIps);
  }

  @JsonProperty(PROP_SRC_OR_DST_IPS)
  public void setSrcOrDstIps(Iterable<IpWildcard> srcOrDstIps) {
    _srcOrDstIps = ImmutableSortedSet.copyOf(srcOrDstIps);
  }

  @JsonProperty(PROP_SRC_OR_DST_PORTS)
  public void setSrcOrDstPorts(Iterable<SubRange> srcOrDstPorts) {
    _srcOrDstPorts = ImmutableSortedSet.copyOf(srcOrDstPorts);
  }

  @JsonProperty(PROP_SRC_OR_DST_PROTOCOLS)
  public void setSrcOrDstProtocols(Iterable<Protocol> srcOrDstProtocols) {
    _srcOrDstProtocols = ImmutableSortedSet.copyOf(srcOrDstProtocols);
  }

  @JsonProperty(PROP_SRC_PORTS)
  public void setSrcPorts(Iterable<SubRange> srcPorts) {
    _srcPorts = ImmutableSortedSet.copyOf(srcPorts);
  }

  @JsonProperty(PROP_SRC_PROTOCOLS)
  public void setSrcProtocols(Iterable<Protocol> srcProtocols) {
    _srcProtocols = ImmutableSortedSet.copyOf(srcProtocols);
  }

  @JsonProperty(PROP_STATES)
  public void setStates(Iterable<State> states) {
    _states = ImmutableSortedSet.copyOf(states);
  }

  @JsonProperty(PROP_TCP_FLAGS)
  public void setTcpFlags(Iterable<TcpFlags> tcpFlags) {
    _tcpFlags = ImmutableList.copyOf(tcpFlags);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(getClass())
        .omitNullValues()
        .add(PROP_DSCPS, nullIfEmpty(_dscps))
        .add(PROP_DST_IPS, nullIfEmpty(_dstIps))
        .add(PROP_DST_PORTS, nullIfEmpty(_dstPorts))
        .add(PROP_DST_PROTOCOLS, nullIfEmpty(_dstProtocols))
        .add(PROP_ECNS, nullIfEmpty(_ecns))
        .add(PROP_FRAGMENT_OFFSETS, nullIfEmpty(_fragmentOffsets))
        .add(PROP_ICMP_CODES, nullIfEmpty(_icmpCodes))
        .add(PROP_ICMP_TYPES, nullIfEmpty(_icmpTypes))
        .add(PROP_IP_PROTOCOLS, nullIfEmpty(_ipProtocols))
        .add(PROP_NEGATE, _negate ? _negate : null)
        .add(PROP_NOT_DSCPS, nullIfEmpty(_notDscps))
        .add(PROP_NOT_DST_IPS, nullIfEmpty(_notDstIps))
        .add(PROP_NOT_DST_PORTS, nullIfEmpty(_notDstPorts))
        .add(PROP_NOT_DST_PROTOCOLS, nullIfEmpty(_notDstProtocols))
        .add(PROP_NOT_ECNS, nullIfEmpty(_notEcns))
        .add(PROP_NOT_FRAGMENT_OFFSETS, nullIfEmpty(_notFragmentOffsets))
        .add(PROP_NOT_ICMP_CODES, nullIfEmpty(_notIcmpCodes))
        .add(PROP_NOT_ICMP_TYPES, nullIfEmpty(_notIcmpTypes))
        .add(PROP_NOT_IP_PROTOCOLS, nullIfEmpty(_notIpProtocols))
        .add(PROP_NOT_PACKET_LENGTHS, nullIfEmpty(_notPacketLengths))
        .add(PROP_NOT_SRC_IPS, nullIfEmpty(_notSrcIps))
        .add(PROP_NOT_SRC_PORTS, nullIfEmpty(_notSrcPorts))
        .add(PROP_NOT_SRC_PROTOCOLS, nullIfEmpty(_notSrcProtocols))
        .add(PROP_PACKET_LENGTHS, nullIfEmpty(_packetLengths))
        .add(PROP_SRC_IPS, nullIfEmpty(_srcIps))
        .add(PROP_SRC_OR_DST_IPS, nullIfEmpty(_srcOrDstIps))
        .add(PROP_SRC_OR_DST_PORTS, nullIfEmpty(_srcOrDstPorts))
        .add(PROP_SRC_OR_DST_PROTOCOLS, nullIfEmpty(_srcOrDstProtocols))
        .add(PROP_SRC_PORTS, nullIfEmpty(_srcPorts))
        .add(PROP_SRC_PROTOCOLS, nullIfEmpty(_srcProtocols))
        .add(PROP_STATES, nullIfEmpty(_states))
        .add(PROP_TCP_FLAGS, nullIfEmpty(_tcpFlags))
        .toString();
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

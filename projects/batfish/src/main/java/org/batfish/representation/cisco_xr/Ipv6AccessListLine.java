package org.batfish.representation.cisco_xr;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Ip6Wildcard;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.TcpFlagsMatchConditions;

@ParametersAreNonnullByDefault
public class Ipv6AccessListLine implements Serializable {

  public static class Builder {

    @Nullable private LineAction _action;

    @Nullable private Set<Integer> _dscps;

    @Nullable private String _dstAddressGroup;

    @Nullable private Ip6Wildcard _dstIpWildcard;

    @Nullable private List<SubRange> _dstPortRanges;

    @Nullable private Integer _icmpCode;

    @Nullable private Integer _icmpType;

    @Nullable private String _name;

    @Nullable private Ipv6Nexthop _nexthop1;

    @Nullable private Ipv6Nexthop _nexthop2;

    @Nullable private Ipv6Nexthop _nexthop3;

    @Nullable private IpProtocol _protocol;

    @Nullable private String _srcAddressGroup;

    @Nullable private Ip6Wildcard _srcIpWildcard;

    @Nullable private List<SubRange> _srcPortRanges;

    @Nullable private List<TcpFlagsMatchConditions> _tcpFlags;

    private Builder() {}

    public Ipv6AccessListLine build() {
      return new Ipv6AccessListLine(this);
    }

    public Ipv6AccessListLine.Builder setAction(LineAction action) {
      _action = action;
      return this;
    }

    public Ipv6AccessListLine.Builder setDscps(Set<Integer> dscps) {
      _dscps = dscps;
      return this;
    }

    public Ipv6AccessListLine.Builder setDstAddressGroup(String dstAddressGroup) {
      _dstAddressGroup = dstAddressGroup;
      return this;
    }

    public Ipv6AccessListLine.Builder setDstIpWildcard(Ip6Wildcard dstIpWildcard) {
      _dstIpWildcard = dstIpWildcard;
      return this;
    }

    public Ipv6AccessListLine.Builder setDstPortRanges(List<SubRange> dstPortRanges) {
      _dstPortRanges = dstPortRanges;
      return this;
    }

    public Ipv6AccessListLine.Builder setIcmpCode(Integer icmpCode) {
      _icmpCode = icmpCode;
      return this;
    }

    public Ipv6AccessListLine.Builder setIcmpType(Integer icmpType) {
      _icmpType = icmpType;
      return this;
    }

    public Ipv6AccessListLine.Builder setName(String name) {
      _name = name;
      return this;
    }

    public Ipv6AccessListLine.Builder setNexthop1(Ipv6Nexthop nexthop) {
      _nexthop1 = nexthop;
      return this;
    }

    public Ipv6AccessListLine.Builder setNexthop2(Ipv6Nexthop nexthop) {
      _nexthop2 = nexthop;
      return this;
    }

    public Ipv6AccessListLine.Builder setNexthop3(Ipv6Nexthop nexthop) {
      _nexthop3 = nexthop;
      return this;
    }

    public Ipv6AccessListLine.Builder setProtocol(IpProtocol protocol) {
      _protocol = protocol;
      return this;
    }

    public Ipv6AccessListLine.Builder setSrcAddressGroup(String srcAddressGroup) {
      _srcAddressGroup = srcAddressGroup;
      return this;
    }

    public Ipv6AccessListLine.Builder setSrcIpWildcard(Ip6Wildcard srcIpWildcard) {
      _srcIpWildcard = srcIpWildcard;
      return this;
    }

    public Ipv6AccessListLine.Builder setSrcPortRanges(List<SubRange> srcPortRanges) {
      _srcPortRanges = srcPortRanges;
      return this;
    }

    public Ipv6AccessListLine.Builder setTcpFlags(List<TcpFlagsMatchConditions> tcpFlags) {
      _tcpFlags = tcpFlags;
      return this;
    }
  }

  public static Ipv6AccessListLine.Builder builder() {
    return new Ipv6AccessListLine.Builder();
  }

  @Nonnull private final LineAction _action;

  @Nonnull private final Set<Integer> _dscps;

  @Nullable private final String _dstAddressGroup;

  @Nonnull private final Ip6Wildcard _dstIpWildcard;

  @Nonnull private final List<SubRange> _dstPortRanges;

  @Nullable private final Integer _icmpCode;

  @Nullable private final Integer _icmpType;

  @Nonnull private final String _name;

  @Nullable private final Ipv6Nexthop _nexthop1;

  @Nullable private final Ipv6Nexthop _nexthop2;

  @Nullable private final Ipv6Nexthop _nexthop3;

  @Nullable private final IpProtocol _protocol;

  @Nullable private final String _srcAddressGroup;

  @Nonnull private final Ip6Wildcard _srcIpWildcard;

  @Nonnull private final List<SubRange> _srcPortRanges;

  @Nonnull private final List<TcpFlagsMatchConditions> _tcpFlags;

  private Ipv6AccessListLine(Ipv6AccessListLine.Builder builder) {
    _action = requireNonNull(builder._action);
    _dscps = ImmutableSet.copyOf(requireNonNull(builder._dscps));
    _dstAddressGroup = builder._dstAddressGroup;
    _dstIpWildcard = requireNonNull(builder._dstIpWildcard);
    _dstPortRanges = ImmutableList.copyOf(requireNonNull(builder._dstPortRanges));
    _icmpCode = builder._icmpCode;
    _icmpType = builder._icmpType;
    _name = requireNonNull(builder._name);
    _nexthop1 = builder._nexthop1;
    _nexthop2 = builder._nexthop2;
    _nexthop3 = builder._nexthop3;
    _protocol = builder._protocol;
    _srcAddressGroup = builder._srcAddressGroup;
    _srcIpWildcard = requireNonNull(builder._srcIpWildcard);
    _srcPortRanges = ImmutableList.copyOf(requireNonNull(builder._srcPortRanges));
    _tcpFlags = ImmutableList.copyOf(requireNonNull(builder._tcpFlags));
  }

  @Nonnull
  public LineAction getAction() {
    return _action;
  }

  @Nonnull
  public Ip6Wildcard getDestinationIpWildcard() {
    return _dstIpWildcard;
  }

  @Nonnull
  public Set<Integer> getDscps() {
    return _dscps;
  }

  @Nullable
  public String getDstAddressGroup() {
    return _dstAddressGroup;
  }

  @Nonnull
  public List<SubRange> getDstPorts() {
    return _dstPortRanges;
  }

  @Nullable
  public Integer getIcmpCode() {
    return _icmpCode;
  }

  @Nullable
  public Integer getIcmpType() {
    return _icmpType;
  }

  @Nonnull
  public String getName() {
    return _name;
  }

  @Nullable
  public Ipv6Nexthop getNexthop1() {
    return _nexthop1;
  }

  @Nullable
  public Ipv6Nexthop getNexthop2() {
    return _nexthop2;
  }

  @Nullable
  public Ipv6Nexthop getNexthop3() {
    return _nexthop3;
  }

  @Nonnull
  public Optional<IpProtocol> getProtocol() {
    return Optional.ofNullable(_protocol);
  }

  @Nonnull
  public Ip6Wildcard getSourceIpWildcard() {
    return _srcIpWildcard;
  }

  @Nullable
  public String getSrcAddressGroup() {
    return _srcAddressGroup;
  }

  @Nonnull
  public List<SubRange> getSrcPorts() {
    return _srcPortRanges;
  }

  @Nonnull
  public List<TcpFlagsMatchConditions> getTcpFlags() {
    return _tcpFlags;
  }

  @Override
  public String toString() {
    return "[Name:\""
        + _name
        + "\", Action:"
        + _action
        + ", Protocol:"
        + (_protocol == null ? "any" : _protocol)
        + ", SourceIpWildcard:"
        + _srcIpWildcard
        + ", DestinationIpWildcard:"
        + _dstIpWildcard
        + ", PortRange:"
        + _srcPortRanges
        + "]";
  }
}

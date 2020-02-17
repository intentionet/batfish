package org.batfish.datamodel;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;

public class MockForwardingAnalysis implements ForwardingAnalysis {

  public static class Builder {

    private Map<String, Map<String, Map<String, IpSpace>>> _acceptedIps;
    private Map<String, Map<String, IpSpace>> _arpReplies;
    private Map<
            String, Map<String, Map<AbstractRoute, Map<String, Map<ArpIpChoice, Set<IpSpace>>>>>>
        _arpRequests;
    private Map<String, Map<String, Map<Edge, IpSpace>>> _arpTrueEdge;
    private Map<String, Map<String, Map<String, IpSpace>>> _deliveredToSubnet;
    private Map<String, Map<String, Map<String, IpSpace>>> _nextVrfIps;
    private Map<String, Map<String, IpSpace>> _nullRoutedIps;
    private Map<String, Map<String, IpSpace>> _routableIps;

    private Builder() {
      _acceptedIps = ImmutableMap.of();
      _arpReplies = ImmutableMap.of();
      _arpRequests = ImmutableMap.of();
      _arpTrueEdge = ImmutableMap.of();
      _deliveredToSubnet = ImmutableMap.of();
      _nextVrfIps = ImmutableMap.of();
      _nullRoutedIps = ImmutableMap.of();
      _routableIps = ImmutableMap.of();
    }

    public MockForwardingAnalysis build() {
      return new MockForwardingAnalysis(this);
    }

    public Builder setAcceptedIps(Map<String, Map<String, Map<String, IpSpace>>> acceptedIps) {
      _acceptedIps = acceptedIps;
      return this;
    }

    public Builder setArpReplies(Map<String, Map<String, IpSpace>> arpReplies) {
      _arpReplies = arpReplies;
      return this;
    }

    public Builder setArpRequests(
        Map<String, Map<String, Map<AbstractRoute, Map<String, Map<ArpIpChoice, Set<IpSpace>>>>>>
            arpRequests) {
      _arpRequests = arpRequests;
      return this;
    }

    public Builder setArpTrueEdge(Map<String, Map<String, Map<Edge, IpSpace>>> arpTrueEdge) {
      _arpTrueEdge = arpTrueEdge;
      return this;
    }

    public Builder setDeliveredToSubnet(
        Map<String, Map<String, Map<String, IpSpace>>> deliveredToSubnet) {
      _deliveredToSubnet = deliveredToSubnet;
      return this;
    }

    public Builder setNextVrfIps(Map<String, Map<String, IpSpace>> nextVrfIps) {
      _nullRoutedIps = nextVrfIps;
      return this;
    }

    public Builder setNullRoutedIps(Map<String, Map<String, IpSpace>> nullRoutedIps) {
      _nullRoutedIps = nullRoutedIps;
      return this;
    }

    public Builder setRoutableIps(Map<String, Map<String, IpSpace>> routableIps) {
      _routableIps = routableIps;
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private final Map<String, Map<String, Map<String, IpSpace>>> _acceptedIps;
  private final Map<String, Map<String, IpSpace>> _arpReplies;
  private final Map<
          String, Map<String, Map<AbstractRoute, Map<String, Map<ArpIpChoice, Set<IpSpace>>>>>>
      _arpRequests;
  private final Map<String, Map<String, Map<Edge, IpSpace>>> _arpTrueEdge;
  private final Map<String, Map<String, Map<String, IpSpace>>> _deliveredToSubnet;
  private final Map<String, Map<String, Map<String, IpSpace>>> _nextVrfIps;
  private final Map<String, Map<String, IpSpace>> _nullRoutedIps;
  private final Map<String, Map<String, IpSpace>> _routableIps;

  public MockForwardingAnalysis(Builder builder) {
    _acceptedIps = ImmutableMap.copyOf(builder._acceptedIps);
    _arpReplies = ImmutableMap.copyOf(builder._arpReplies);
    _arpRequests = ImmutableMap.copyOf(builder._arpRequests);
    _arpTrueEdge = ImmutableMap.copyOf(builder._arpTrueEdge);
    _deliveredToSubnet = ImmutableMap.copyOf(builder._deliveredToSubnet);
    _nextVrfIps = ImmutableMap.copyOf(builder._nextVrfIps);
    _nullRoutedIps = ImmutableMap.copyOf(builder._nullRoutedIps);
    _routableIps = ImmutableMap.copyOf(builder._routableIps);
  }

  @Nonnull
  @Override
  public Map<String, Map<String, Map<String, IpSpace>>> getAcceptsIps() {
    return _acceptedIps;
  }

  @Override
  public Map<String, Map<String, IpSpace>> getArpReplies() {
    return _arpReplies;
  }

  public Map<String, Map<String, Map<AbstractRoute, Map<String, Map<ArpIpChoice, Set<IpSpace>>>>>>
      getArpRequests() {
    return _arpRequests;
  }

  @Override
  public Map<String, Map<String, Map<Edge, IpSpace>>> getArpTrueEdge() {
    return _arpTrueEdge;
  }

  @Override
  public Map<String, Map<String, Map<String, IpSpace>>> getNeighborUnreachable() {
    return ImmutableMap.of();
  }

  @Override
  public Map<String, Map<String, Map<String, IpSpace>>> getExitsNetwork() {
    return ImmutableMap.of();
  }

  @Override
  public Map<String, Map<String, Map<String, IpSpace>>> getDeliveredToSubnet() {
    return _deliveredToSubnet;
  }

  @Override
  public Map<String, Map<String, Map<String, IpSpace>>> getInsufficientInfo() {
    return ImmutableMap.of();
  }

  @Override
  public Map<String, Map<String, IpSpace>> getNullRoutedIps() {
    return _nullRoutedIps;
  }

  @Override
  public Map<String, Map<String, IpSpace>> getRoutableIps() {
    return _routableIps;
  }

  @Override
  public Map<String, Map<String, Map<String, IpSpace>>> getNextVrfIps() {
    return _nextVrfIps;
  }
}

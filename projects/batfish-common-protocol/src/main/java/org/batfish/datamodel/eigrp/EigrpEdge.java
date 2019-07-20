package org.batfish.datamodel.eigrp;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Optional.empty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.collections.IpEdge;

public final class EigrpEdge implements Serializable, Comparable<EigrpEdge> {
  private static final String PROP_NODE1 = "node1";
  private static final String PROP_NODE2 = "node2";

  @Nonnull private final EigrpNeighborConfigId _node1;
  @Nonnull private final EigrpNeighborConfigId _node2;

  @JsonCreator
  public EigrpEdge(
      @Nonnull @JsonProperty(PROP_NODE1) EigrpNeighborConfigId node1,
      @Nonnull @JsonProperty(PROP_NODE2) EigrpNeighborConfigId node2) {
    _node1 = node1;
    _node2 = node2;
  }

  /** Return an {@link EigrpEdge} if there is an EIGRP adjacency on {@code edge}. */
  @Nonnull
  static Optional<EigrpEdge> edgeIfAdjacent(Edge edge, Map<String, Configuration> configurations) {
    // vertex1
    Configuration c1 = configurations.get(edge.getNode1());
    Interface iface1 = c1.getAllInterfaces().get(edge.getInt1());
    EigrpInterfaceSettings eigrp1 = iface1.getEigrp();

    // vertex2
    Configuration c2 = configurations.get(edge.getNode2());
    Interface iface2 = c2.getAllInterfaces().get(edge.getInt2());
    EigrpInterfaceSettings eigrp2 = iface2.getEigrp();

    if (eigrp1 == null
        || eigrp2 == null
        || !eigrp1.getEnabled()
        || !eigrp2.getEnabled()
        || eigrp1.getAsn() != eigrp2.getAsn()
        || eigrp1.getPassive()
        || eigrp2.getPassive()
        || eigrp1.getMetric().getMode() != eigrp2.getMetric().getMode()) {
      return empty();
    }
    return Optional.of(
        new EigrpEdge(
            new EigrpNeighborConfigId(c1.getHostname(), iface1),
            new EigrpNeighborConfigId(c2.getHostname(), iface2)));
  }

  @Override
  public int compareTo(@Nonnull EigrpEdge o) {
    return Comparator.comparing(EigrpEdge::getNode1)
        .thenComparing(EigrpEdge::getNode2)
        .compare(this, o);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EigrpEdge)) {
      return false;
    }
    EigrpEdge rhs = (EigrpEdge) o;
    return _node1.equals(rhs._node1) && _node2.equals(rhs._node2);
  }

  @Nonnull
  public IpEdge toIpEdge(NetworkConfigurations nc) {
    return new IpEdge(
        _node1.getHostname(),
        _node1.getInterface(nc).getConcreteAddress().getIp(),
        _node2.getHostname(),
        _node2.getInterface(nc).getConcreteAddress().getIp());
  }

  @Nonnull
  @JsonProperty(PROP_NODE1)
  public EigrpNeighborConfigId getNode1() {
    return _node1;
  }

  @Nonnull
  @JsonProperty(PROP_NODE2)
  public EigrpNeighborConfigId getNode2() {
    return _node2;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_node1, _node2);
  }

  @Nonnull
  public EigrpEdge reverse() {
    return new EigrpEdge(_node2, _node1);
  }

  @Override
  public String toString() {
    return toStringHelper(getClass()).add("node1", _node1).add("node2", _node2).toString();
  }
}

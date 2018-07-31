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
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.collections.IpEdge;

public final class EigrpEdge implements Serializable, Comparable<EigrpEdge> {

  private static final String PROP_NODE1 = "node1";
  private static final String PROP_NODE2 = "node2";
  private static final long serialVersionUID = 1L;

  @Nonnull private final EigrpInterface _node1;
  @Nonnull private final EigrpInterface _node2;

  @JsonCreator
  public EigrpEdge(
      @Nonnull @JsonProperty(PROP_NODE1) EigrpInterface node1,
      @Nonnull @JsonProperty(PROP_NODE2) EigrpInterface node2) {
    _node1 = node1;
    _node2 = node2;
  }

  /** Return an {@link EigrpEdge} if there is an EIGRP adjacency on {@code edge}. */
  @Nonnull
  static Optional<EigrpEdge> edgeIfAdjacent(Edge edge, Map<String, Configuration> configurations) {
    // vertex1
    Configuration c1 = configurations.get(edge.getNode1());
    Interface iface1 = c1.getInterfaces().get(edge.getInt1());
    Vrf vrf1 = iface1.getVrf();
    EigrpProcess proc1 = vrf1.getEigrpProcess();
    if (proc1 == null) {
      return empty();
    }
    EigrpInterfaceSettings eigrp1 = iface1.getEigrp();
    if (eigrp1 == null) {
      return empty();
    }

    // vertex2
    Configuration c2 = configurations.get(edge.getNode2());
    Interface iface2 = c2.getInterfaces().get(edge.getInt2());
    Vrf vrf2 = iface2.getVrf();
    EigrpProcess proc2 = vrf2.getEigrpProcess();
    if (proc2 == null) {
      return empty();
    }
    EigrpInterfaceSettings eigrp2 = iface2.getEigrp();
    if (eigrp2 == null) {
      return empty();
    }

    if (!eigrp1.getEnabled()
        || !eigrp2.getEnabled()
        || !Objects.equals(eigrp1.getAsNumber(), eigrp2.getAsNumber())
        || eigrp1.getPassive()
        || eigrp2.getPassive()) {
      return empty();
    }
    return Optional.of(
        new EigrpEdge(
            new EigrpInterface(c1.getHostname(), iface1.getName(), vrf1.getName()),
            new EigrpInterface(c2.getHostname(), iface2.getName(), vrf2.getName())));
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
        _node1.getInterface(nc).getAddress().getIp(),
        _node2.getHostname(),
        _node2.getInterface(nc).getAddress().getIp());
  }

  @Nonnull
  @JsonProperty(PROP_NODE1)
  public EigrpInterface getNode1() {
    return _node1;
  }

  @Nonnull
  @JsonProperty(PROP_NODE2)
  public EigrpInterface getNode2() {
    return _node2;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_node1, _node2);
  }

  @Override
  public String toString() {
    return toStringHelper(getClass()).add("node1", _node1).add("node2", _node2).toString();
  }
}

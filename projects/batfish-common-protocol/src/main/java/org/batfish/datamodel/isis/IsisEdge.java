package org.batfish.datamodel.isis;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Optional.empty;
import static org.batfish.datamodel.isis.IsisLevel.LEVEL_1;
import static org.batfish.datamodel.isis.IsisLevel.LEVEL_1_2;
import static org.batfish.datamodel.isis.IsisLevel.LEVEL_2;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Vrf;

public final class IsisEdge implements Comparable<IsisEdge> {

  @Nullable
  private static IsisLevel interfaceSettingsLevel(IsisInterfaceSettings settings) {
    return union(
        settings.getLevel1() != null ? LEVEL_1 : null,
        settings.getLevel2() != null ? LEVEL_2 : null);
  }

  @Nullable
  private static IsisLevel intersection(IsisLevel... levels) {
    return levels.length == 0 ? null : Arrays.stream(levels).reduce(IsisEdge::intersection).get();
  }

  @Nullable
  private static IsisLevel intersection(@Nullable IsisLevel first, @Nullable IsisLevel second) {
    if (first == second) {
      return first;
    }
    if (first == LEVEL_1_2) {
      return second;
    }
    if (second == LEVEL_1_2) {
      return first;
    }
    return null;
  }

  /** Return an {@link IsisEdge} if there is an IS-IS circuit on {@code edge}. */
  @Nonnull
  public static Optional<IsisEdge> edgeIfCircuit(
      Edge edge, Map<String, Configuration> configurations) {
    // vertex1
    Configuration c1 = configurations.get(edge.getNode1());
    Interface iface1 = c1.getInterfaces().get(edge.getInt1());
    Vrf vrf1 = iface1.getVrf();
    IsisProcess proc1 = vrf1.getIsisProcess();
    if (proc1 == null) {
      return empty();
    }
    IsisInterfaceSettings is1 = iface1.getIsis();
    if (is1 == null) {
      return empty();
    }

    // vertex2
    Configuration c2 = configurations.get(edge.getNode2());
    Interface iface2 = c2.getInterfaces().get(edge.getInt2());
    Vrf vrf2 = iface2.getVrf();
    IsisProcess proc2 = vrf2.getIsisProcess();
    if (proc2 == null) {
      return empty();
    }
    IsisInterfaceSettings is2 = iface2.getIsis();
    if (is2 == null) {
      return empty();
    }

    boolean sameArea =
        Arrays.equals(proc1.getNetAddress().getAreaId(), proc2.getNetAddress().getAreaId());

    // make sure system IDs are distinct
    if (Arrays.equals(proc1.getNetAddress().getSystemId(), proc2.getNetAddress().getSystemId())) {
      return empty();
    }

    /* Compute level. If areas are distinct, only level 2 is allowed. */
    IsisLevel circuitType =
        intersection(
            processLevel(proc1),
            interfaceSettingsLevel(is1),
            processLevel(proc2),
            interfaceSettingsLevel(is2),
            sameArea ? LEVEL_1_2 : LEVEL_2);
    if (circuitType == null) {
      return empty();
    }

    return Optional.of(
        new IsisEdge(
            circuitType,
            new IsisNode(c1.getHostname(), iface1.getName()),
            new IsisNode(c2.getHostname(), iface2.getName())));
  }

  @Nullable
  private static IsisLevel processLevel(IsisProcess isisProcess) {
    return union(
        isisProcess.getLevel1() != null ? LEVEL_1 : null,
        isisProcess.getLevel2() != null ? LEVEL_2 : null);
  }

  @Nullable
  private static IsisLevel union(@Nullable IsisLevel level1, @Nullable IsisLevel level2) {
    if (level1 == level2) {
      return level1;
    }
    if (level1 == LEVEL_1_2 || level2 == LEVEL_1_2) {
      return LEVEL_1_2;
    }
    if (level1 == LEVEL_1 || level2 == LEVEL_1) {
      // other is null or LEVEL_2
      if (level1 == LEVEL_2 || level2 == LEVEL_2) {
        return LEVEL_1_2;
      }
      // other is null
      return LEVEL_1;
    }
    // one is null and one is LEVEL_2
    return LEVEL_2;
  }

  private final IsisLevel _circuitType;

  private final IsisNode _node1;

  private final IsisNode _node2;

  public IsisEdge(
      @Nonnull IsisLevel circuitType, @Nonnull IsisNode node1, @Nonnull IsisNode node2) {
    _circuitType = circuitType;
    _node1 = node1;
    _node2 = node2;
  }

  @Override
  public int compareTo(@Nonnull IsisEdge o) {
    return Comparator.comparing(IsisEdge::getNode1)
        .thenComparing(IsisEdge::getNode2)
        .thenComparing(IsisEdge::getCircuitType)
        .compare(this, o);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof IsisEdge)) {
      return false;
    }
    IsisEdge rhs = (IsisEdge) o;
    return _circuitType == rhs._circuitType
        && _node1.equals(rhs._node1)
        && _node2.equals(rhs._node2);
  }

  @Nonnull
  public IsisLevel getCircuitType() {
    return _circuitType;
  }

  @Nonnull
  public IsisNode getNode1() {
    return _node1;
  }

  @Nonnull
  public IsisNode getNode2() {
    return _node2;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_circuitType.ordinal(), _node1, _node2);
  }

  @Nonnull
  public IsisEdge reverse() {
    return new IsisEdge(_circuitType, _node2, _node1);
  }

  @Override
  public String toString() {
    return toStringHelper(getClass())
        .add("node1", _node1)
        .add("node2", _node2)
        .add("circuitType", _circuitType)
        .toString();
  }
}

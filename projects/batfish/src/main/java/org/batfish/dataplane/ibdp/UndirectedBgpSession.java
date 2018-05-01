package org.batfish.dataplane.ibdp;

import java.util.Comparator;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpSession;

/**
 * Represents an undirected peering between two BGP neighbors, with deterministic ordering of peers.
 * {@link UndirectedBgpSession} is immutable.
 */
public class UndirectedBgpSession implements Comparable<UndirectedBgpSession> {
  @Nonnull private final BgpNeighbor _first;
  @Nonnull private final BgpNeighbor _second;
  private static final BgpNeighborComparator _comparator = new BgpNeighborComparator();

  /**
   * Compare BGP neighbors using prefix and hostname. This comparator is **NOT** consistent with
   * equals
   */
  private static class BgpNeighborComparator implements Comparator<BgpNeighbor> {
    @Override
    public int compare(BgpNeighbor o1, BgpNeighbor o2) {
      return Comparator.comparing(BgpNeighbor::getPrefix)
          .thenComparing(BgpNeighbor::getOwner)
          .compare(o1, o2);
    }
  }

  UndirectedBgpSession(@Nonnull BgpNeighbor n1, @Nonnull BgpNeighbor n2) {
    if (_comparator.compare(n1, n2) < 0) {
      _first = n1;
      _second = n2;
    } else {
      _first = n2;
      _second = n1;
    }
  }

  @Nonnull
  public BgpNeighbor getFirst() {
    return _first;
  }

  @Nonnull
  public BgpNeighbor getSecond() {
    return _second;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UndirectedBgpSession other = (UndirectedBgpSession) o;
    return Objects.equals(getFirst().getPrefix(), other.getFirst().getPrefix())
        && Objects.equals(getFirst().getOwner(), other.getFirst().getOwner())
        && Objects.equals(getSecond().getPrefix(), other.getSecond().getPrefix())
        && Objects.equals(getSecond().getOwner(), other.getSecond().getOwner());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getFirst(), getFirst().getOwner(), getSecond(), getSecond().getOwner());
  }

  @Override
  public int compareTo(@Nonnull UndirectedBgpSession o) {
    return Comparator.comparing(UndirectedBgpSession::getFirst, _comparator)
        .thenComparing(UndirectedBgpSession::getSecond, _comparator)
        .compare(this, o);
  }

  public static UndirectedBgpSession from(@Nonnull BgpSession session) {
    return new UndirectedBgpSession(session.getSrc(), session.getDst());
  }

  public static UndirectedBgpSession from(@Nonnull BgpNeighbor n1, @Nonnull BgpNeighbor n2) {
    return new UndirectedBgpSession(n1, n2);
  }
}

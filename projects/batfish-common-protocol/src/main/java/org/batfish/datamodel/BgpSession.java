package org.batfish.datamodel;

import java.util.Comparator;
import java.util.Objects;
import javax.annotation.Nonnull;

/** Represents a peering session between two {@link BgpNeighbor}s */
public class BgpSession implements Comparable<BgpSession> {

  private final boolean _isEbgp;

  @Nonnull private BgpNeighbor _src;
  @Nonnull private BgpNeighbor _dst;

  /**
   * Create a new session
   *
   * @param src session initiator
   * @param dst session acceptor
   */
  public BgpSession(@Nonnull BgpNeighbor src, @Nonnull BgpNeighbor dst) {
    _src = src;
    _dst = dst;
    _isEbgp = !src.getLocalAs().equals(dst.getLocalAs());
  }

  public BgpNeighbor getSrc() {
    return _src;
  }

  public BgpNeighbor getDst() {
    return _dst;
  }

  public boolean isEbgp() {
    return _isEbgp;
  }

  /**
   * When this is set, add best eBGP path independently of whether it is preempted by an iBGP or IGP
   * route. Only applicable to iBGP sessions.
   */
  public boolean getAdvertiseExternal() {
    return !_isEbgp && _src.getAdvertiseExternal();
  }

  /**
   * When this is set, add best BGP path independently of whether it is preempted by an IGP route.
   * Only applicable to eBGP sessions.
   */
  public boolean getAdvertiseInactive() {
    return _isEbgp && _src.getAdvertiseInactive();
  }

  /** When this is set, advertise all paths from the multipath RIB */
  public boolean getAdditionalPaths() {
    return !_isEbgp
        && _dst.getAdditionalPathsReceive()
        && _src.getAdditionalPathsSend()
        && _src.getAdditionalPathsSelectAll();
  }

  @Override
  public int compareTo(@Nonnull BgpSession o) {
    return Comparator.comparing((BgpSession s) -> s.getSrc().getPrefix())
        .thenComparing(s -> s.getSrc().getOwner())
        .thenComparing(s -> s.getDst().getOwner())
        .compare(this, o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_src, _dst);
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj
        || ((obj instanceof BgpSession)
            && _src.equals(((BgpSession) obj)._src)
            && _dst.equals(((BgpSession) obj)._dst));
  }
}

package org.batfish.datamodel;

import java.util.Objects;
import javax.annotation.Nonnull;

/** Represents properties of a peering session between two {@link BgpPeerConfig}s. */
public final class BgpSessionProperties {

  private final boolean _additionalPaths;
  private final boolean _advertiseExternal;
  private final boolean _advertiseInactive;
  private final SessionType _sessionType;

  private BgpSessionProperties(
      boolean additionalPaths,
      boolean advertiseExternal,
      boolean advertiseInactive,
      SessionType sessionType) {
    _additionalPaths = additionalPaths;
    _advertiseExternal = advertiseExternal;
    _advertiseInactive = advertiseInactive;
    _sessionType = sessionType;
  }

  /**
   * When this is set, add best eBGP path independently of whether it is preempted by an iBGP or IGP
   * route. Only applicable to iBGP sessions.
   */
  public boolean getAdvertiseExternal() {
    return _advertiseExternal;
  }

  /**
   * When this is true, add best BGP path independently of whether it is preempted by an IGP route.
   * Only applicable to eBGP sessions.
   */
  public boolean getAdvertiseInactive() {
    return _advertiseInactive;
  }

  /** When this is true, advertise all paths from the multipath RIB */
  public boolean getAdditionalPaths() {
    return _additionalPaths;
  }

  /** Whether this session is eBGP. */
  public boolean isEbgp() {
    return SessionType.isEbgp(_sessionType);
  }

  /** Return this session's {@link SessionType}. */
  public SessionType getSessionType() {
    return _sessionType;
  }

  /**
   * Create a set of new parameters based on session initiator and listener. <b>Note</b> that some
   * parameters (such as {@link #isEbgp()} will be determined based on the configuration of the
   * initiator only.
   */
  public static BgpSessionProperties from(
      @Nonnull BgpActivePeerConfig initiator, @Nonnull BgpPeerConfig listener) {
    return from(initiator, listener, getSessionType(initiator));
  }

  /**
   * Determine what type of session the peer is configured to establish.
   *
   * @param initiator the configuration of connection initiator.
   * @return a {@link SessionType} the initiator is configured to establish.
   */
  public static @Nonnull SessionType getSessionType(BgpActivePeerConfig initiator) {
    SessionType sessionType = SessionType.UNSET;
    if (initiator.getLocalAs() != null && initiator.getRemoteAs() != null) {
      if (initiator.getLocalAs().equals(initiator.getRemoteAs())) {
        sessionType = SessionType.IBGP;
      } else if (initiator.getEbgpMultihop()) {
        sessionType = SessionType.EBGP_MULTIHOP;
      } else {
        sessionType = SessionType.EBGP_SINGLEHOP;
      }
    }
    return sessionType;
  }

  private static BgpSessionProperties from(
      @Nonnull BgpPeerConfig initiator, @Nonnull BgpPeerConfig listener, SessionType sessionType) {

    return new BgpSessionProperties(
        !SessionType.isEbgp(sessionType)
            && listener.getAdditionalPathsReceive()
            && initiator.getAdditionalPathsSend()
            && initiator.getAdditionalPathsSelectAll(),
        SessionType.isEbgp(sessionType) && initiator.getAdvertiseInactive(),
        !SessionType.isEbgp(sessionType) && initiator.getAdvertiseExternal(),
        sessionType);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BgpSessionProperties)) {
      return false;
    }
    BgpSessionProperties that = (BgpSessionProperties) o;
    return _additionalPaths == that._additionalPaths
        && _advertiseExternal == that._advertiseExternal
        && _advertiseInactive == that._advertiseInactive
        && _sessionType == that._sessionType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_additionalPaths, _advertiseExternal, _advertiseInactive, _sessionType);
  }

  public enum SessionType {
    IBGP,
    EBGP_SINGLEHOP,
    EBGP_MULTIHOP,
    UNSET;

    public static boolean isEbgp(SessionType sessionType) {
      return sessionType == SessionType.EBGP_SINGLEHOP || sessionType == SessionType.EBGP_MULTIHOP;
    }
  }
}

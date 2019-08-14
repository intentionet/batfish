package org.batfish.representation.cisco_nxos;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.Ip;

/** HSRP group within {@link InterfaceHsrp} settings for an {@link Interface}. */
public final class HsrpGroup implements Serializable {

  public HsrpGroup(int group) {
    _group = group;
    _ipSecondaries = new HashSet<>();
    _tracks = new HashMap<>();
  }

  public @Nullable Integer getHelloIntervalMs() {
    return _helloIntervalMs;
  }

  public void setHelloIntervalMs(@Nullable Integer helloIntervalMs) {
    _helloIntervalMs = helloIntervalMs;
  }

  public @Nullable Integer getHoldTimeMs() {
    return _holdTimeMs;
  }

  public void setHoldTimeMs(@Nullable Integer holdTimeMs) {
    _holdTimeMs = holdTimeMs;
  }

  public @Nullable Ip getIp() {
    return _ip;
  }

  public void setIp(@Nullable Ip ip) {
    _ip = ip;
  }

  public @Nonnull Set<Ip> getIpSecondaries() {
    return _ipSecondaries;
  }

  public @Nullable Integer getPreemptDelayMinimumSeconds() {
    return _preemptDelayMinimumSeconds;
  }

  public void setPreemptDelayMinimumSeconds(@Nullable Integer preemptDelayMinimumSeconds) {
    _preemptDelayMinimumSeconds = preemptDelayMinimumSeconds;
  }

  public @Nullable Integer getPreemptDelayReloadSeconds() {
    return _preemptDelayReloadSeconds;
  }

  public void setPreemptDelayReloadSeconds(@Nullable Integer preemptDelayReloadSeconds) {
    _preemptDelayReloadSeconds = preemptDelayReloadSeconds;
  }

  public @Nullable Integer getPreemptDelaySyncSeconds() {
    return _preemptDelaySyncSeconds;
  }

  public void setPreemptDelaySyncSeconds(@Nullable Integer preemptDelaySyncSeconds) {
    _preemptDelaySyncSeconds = preemptDelaySyncSeconds;
  }

  public @Nullable Integer getPriority() {
    return _priority;
  }

  public void setPriority(@Nullable Integer priority) {
    _priority = priority;
  }

  public int getGroup() {
    return _group;
  }

  public @Nonnull Map<Integer, HsrpTrack> getTracks() {
    return _tracks;
  }

  //////////////////////////////////////////
  ///// Private implementation details /////
  //////////////////////////////////////////

  private final int _group;
  private @Nullable Integer _helloIntervalMs;
  private @Nullable Integer _holdTimeMs;
  private @Nullable Ip _ip;
  private final @Nonnull Set<Ip> _ipSecondaries;
  private @Nullable Integer _preemptDelayMinimumSeconds;
  private @Nullable Integer _preemptDelayReloadSeconds;
  private @Nullable Integer _preemptDelaySyncSeconds;
  private @Nullable Integer _priority;
  private final @Nonnull Map<Integer, HsrpTrack> _tracks;
}

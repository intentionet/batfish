package org.batfish.representation.cisco_nxos;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.io.Serializable;
import javax.annotation.Nullable;

public final class OspfInterface implements Serializable {

  // https://www.cisco.com/c/m/en_us/techdoc/dc/reference/cli/nxos/commands/ospf/ip-ospf-dead-interval.html
  public static final int DEFAULT_DEAD_INTERVAL_S = 40; // s
  public static final int DEFAULT_HELLO_INTERVAL_S = 10; // s

  public @Nullable Long getArea() {
    return _area;
  }

  public void setArea(@Nullable Long area) {
    _area = area;
  }

  public boolean getBfd() {
    return _bfd;
  }

  public void setBfd(boolean bfd) {
    _bfd = bfd;
  }

  public @Nullable Integer getCost() {
    return _cost;
  }

  public void setCost(@Nullable Integer cost) {
    _cost = cost;
  }

  public int getDeadIntervalS() {
    return firstNonNull(_deadIntervalS, DEFAULT_DEAD_INTERVAL_S);
  }

  public void setDeadIntervalS(int deadInterval) {
    _deadIntervalS = deadInterval;
  }

  public int getHelloIntervalS() {
    return firstNonNull(_helloIntervalS, DEFAULT_HELLO_INTERVAL_S);
  }

  public void setHelloIntervalS(int helloIntervalS) {
    _helloIntervalS = helloIntervalS;
  }

  public @Nullable OspfNetworkType getNetwork() {
    return _network;
  }

  public void setNetwork(@Nullable OspfNetworkType network) {
    _network = network;
  }

  public @Nullable Boolean getPassive() {
    return _passive;
  }

  public void setPassive(@Nullable Boolean passive) {
    _passive = passive;
  }

  public @Nullable String getProcess() {
    return _process;
  }

  public void setProcess(@Nullable String process) {
    _process = process;
  }

  //////////////////////////////////////////
  ///// Private implementation details /////
  //////////////////////////////////////////

  private @Nullable Long _area;
  private boolean _bfd;
  private @Nullable Integer _cost;
  private @Nullable Integer _deadIntervalS;
  private @Nullable Integer _helloIntervalS;
  private @Nullable OspfNetworkType _network;
  private @Nullable Boolean _passive;
  private @Nullable String _process;
}

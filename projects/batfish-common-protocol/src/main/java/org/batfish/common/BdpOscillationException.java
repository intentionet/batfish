package org.batfish.common;

/** Indicates that there is no stable dataplane solution */
public class BdpOscillationException extends BatfishException {

  private static final long serialVersionUID = 1L;

  public BdpOscillationException(String msg) {
    super(msg);
  }

  public BdpOscillationException(String msg, Throwable cause) {
    super(msg, cause);
  }
}

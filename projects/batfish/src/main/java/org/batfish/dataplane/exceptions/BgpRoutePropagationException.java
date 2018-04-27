package org.batfish.dataplane.exceptions;

/**
 * Thrown if the dataplane fails to propagate a BGP route between two routers due to missing info
 */
public class BgpRoutePropagationException extends Exception {

  private static final long serialVersionUID = 1L;

  public BgpRoutePropagationException(String s) {
    super(s);
  }
}

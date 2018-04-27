package org.batfish.dataplane.exceptions;

import org.batfish.common.BatfishException;

/** Thrown when best route selection fails */
public class BestPathSelectionException extends BatfishException {

  private static final long serialVersionUID = 1L;

  public BestPathSelectionException(String msg) {
    super(msg);
  }
}

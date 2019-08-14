package org.batfish.representation.cisco_nxos;

import java.io.Serializable;

/** LACP settings for an {@link Interface}. */
public final class Lacp implements Serializable {

  public static final int DEFAULT_MIN_LINKS = 1;

  public Lacp() {
    _minLinks = DEFAULT_MIN_LINKS;
  }

  public int getMinLinks() {
    return _minLinks;
  }

  public void setMinLinks(int minLinks) {
    _minLinks = minLinks;
  }

  //////////////////////////////////////////
  ///// Private implementation details /////
  //////////////////////////////////////////

  private int _minLinks;
}

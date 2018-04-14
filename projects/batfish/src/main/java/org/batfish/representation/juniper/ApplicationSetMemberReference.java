package org.batfish.representation.juniper;

import java.io.Serializable;

public abstract class ApplicationSetMemberReference implements Serializable {

  /** */
  private static final long serialVersionUID = 1L;

  protected final String _name;

  protected ApplicationSetMemberReference(String name) {
    _name = name;
  }

  public abstract ApplicationSetMember resolve(JuniperConfiguration jc);
}

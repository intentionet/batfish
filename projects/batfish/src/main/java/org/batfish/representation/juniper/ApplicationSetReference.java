package org.batfish.representation.juniper;

public class ApplicationSetReference extends ApplicationSetMemberReference {

  /** */
  private static final long serialVersionUID = 1L;

  public ApplicationSetReference(String name) {
    super(name);
  }

  @Override
  public ApplicationSetMember resolve(JuniperConfiguration jc) {
    return jc.getApplicationSets().get(_name);
  }
}

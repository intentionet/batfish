package org.batfish.representation.juniper;

public class ApplicationReference extends ApplicationSetMemberReference {

  /** */
  private static final long serialVersionUID = 1L;

  public ApplicationReference(String name) {
    super(name);
  }

  @Override
  public ApplicationSetMember resolve(JuniperConfiguration jc) {
    return jc.getApplications().get(_name);
  }
}

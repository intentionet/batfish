package org.batfish.datamodel;

import org.batfish.datamodel.visitors.GenericIpSpaceVisitor;

public class EmptyIpSpace implements JacksonSerializableIpSpace {

  public static final IpSpace INSTANCE = new EmptyIpSpace();

  private EmptyIpSpace() {}

  @Override
  public <R> R accept(GenericIpSpaceVisitor<R> visitor) {
    return visitor.visitEmptyIpSpace(this);
  }

  @Override
  public boolean containsIp(Ip ip) {
    return false;
  }

  @Override
  public IpSpace complement() {
    return UniverseIpSpace.INSTANCE;
  }

  @Override
  public IpSpace unwrap() {
    return this;
  }

  @Override
  public String toString() {
    return "empty";
  }

  @Override
  public boolean equals(Object obj) {
    return obj == INSTANCE || obj instanceof EmptyIpSpace;
  }

  @Override
  public int hashCode() {
    return getClass().getCanonicalName().hashCode();
  }
}

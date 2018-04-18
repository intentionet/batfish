package org.batfish.datamodel;

import javax.annotation.Nonnull;
import org.batfish.datamodel.visitors.GenericIpSpaceVisitor;

public class UniverseIpSpace implements JacksonSerializableIpSpace {

  public static final UniverseIpSpace INSTANCE = new UniverseIpSpace();

  private UniverseIpSpace() {}

  @Override
  public <R> R accept(GenericIpSpaceVisitor<R> ipSpaceVisitor) {
    return ipSpaceVisitor.visitUniverseIpSpace(this);
  }

  @Override
  public IpSpace complement() {
    return EmptyIpSpace.INSTANCE;
  }

  @Override
  public boolean containsIp(@Nonnull Ip ip) {
    return true;
  }

  @Override
  public String toString() {
    return "universe";
  }

  @Override
  public boolean equals(Object obj) {
    return obj == INSTANCE || obj instanceof UniverseIpSpace;
  }

  @Override
  public int hashCode() {
    return getClass().getCanonicalName().hashCode();
  }

  @Override
  public IpSpace unwrap() {
    return this;
  }
}

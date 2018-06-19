package org.batfish.datamodel;

import javax.annotation.Nonnull;

public class IsisRoute extends AbstractRoute {

  public static class Builder extends AbstractRouteBuilder<Builder, IsisRoute> {

    @Override
    public IsisRoute build() {
      throw new UnsupportedOperationException("no implementation for generated method");
      // TODO Auto-generated method stub
    }

    @Override
    protected Builder getThis() {
      return this;
    }

    public void setLevel(IsisLevel level) {
      throw new UnsupportedOperationException("no implementation for generated method");
      // TODO Auto-generated method stub
    }
  }

  /** */
  private static final long serialVersionUID = 1L;

  public IsisRoute(Prefix network) {
    super(network);
    // TODO Auto-generated constructor stub
  }

  @Override
  public boolean equals(Object o) {
    throw new UnsupportedOperationException("no implementation for generated method");
    // TODO Auto-generated method stub
  }

  @Override
  public int getAdministrativeCost() {
    throw new UnsupportedOperationException("no implementation for generated method");
    // TODO Auto-generated method stub
  }

  @Override
  public Long getMetric() {
    throw new UnsupportedOperationException("no implementation for generated method");
    // TODO Auto-generated method stub
  }

  @Nonnull
  @Override
  public String getNextHopInterface() {
    throw new UnsupportedOperationException("no implementation for generated method");
    // TODO Auto-generated method stub
  }

  @Nonnull
  @Override
  public Ip getNextHopIp() {
    throw new UnsupportedOperationException("no implementation for generated method");
    // TODO Auto-generated method stub
  }

  @Override
  public RoutingProtocol getProtocol() {
    throw new UnsupportedOperationException("no implementation for generated method");
    // TODO Auto-generated method stub
  }

  @Override
  public int getTag() {
    throw new UnsupportedOperationException("no implementation for generated method");
    // TODO Auto-generated method stub
  }

  @Override
  public int hashCode() {
    throw new UnsupportedOperationException("no implementation for generated method");
    // TODO Auto-generated method stub
  }

  @Override
  public String protocolRouteString() {
    throw new UnsupportedOperationException("no implementation for generated method");
    // TODO Auto-generated method stub
  }

  @Override
  public int routeCompare(@Nonnull AbstractRoute rhs) {
    throw new UnsupportedOperationException("no implementation for generated method");
    // TODO Auto-generated method stub
  }
}

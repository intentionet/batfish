package org.batfish.datamodel;

/** A {@link FibAction} that discards a packet. */
public final class FibNullRoute implements FibAction {

  private static final long serialVersionUID = 1L;

  public static final FibNullRoute INSTANCE = new FibNullRoute();

  private FibNullRoute() {}

  @Override
  public FibActionType getType() {
    return FibActionType.NULL_ROUTE;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof FibNullRoute;
  }

  @Override
  public int hashCode() {
    return 0;
  }
}
